# Telegram and study sessions: how it works

For whoever has to change or explain this code. Paths are under `backend/src/main/java/is442t1/studybuddy/`.

## The big picture

The app must work with Telegram off, and a Telegram outage must never break or slow StudyBuddy. Almost every decision below follows from that.

```
Browser ──> Controller ──> Service ──> Database
                              │ publishes an event
                              ▼ after the commit, on a background thread
                       ChatSyncListener ──> SessionChatService / GroupChatService ──> Telegram

Telegram ──> TelegramUpdatePoller ──> UpdateClassifier ──> TelegramUpdateRouter ──> handlers ──> Database and Telegram

Timer ──> SessionCardJob ──> SessionChatService ──> Telegram
Timer ──> SessionCardRefresher ──> Telegram   (the only thing that posts or edits session cards)
```

So there are three ways in: a person using the app, a person using Telegram, and a timer.

| Package | What it holds |
|---|---|
| `telegram/api/` | Talks to Telegram. `TelegramClient` is the interface, `HttpTelegramClient` the real one, `DisabledTelegramClient` the one used when Telegram is off. |
| `telegram/bot/` | Reads updates and decides what each one means. `BotEvent` is a sealed interface, so the compiler checks the router handles every kind. |
| `telegram/link/` | Connecting a student's Telegram account, and the one-time links. |
| `telegram/chat/` | Group chats, invites, session posts and reminders. |
| `studysession/` | Sessions, RSVPs and calendar files. It knows nothing about Telegram. |

`telegram/` depends on `group/` and `studysession/`, and never the other way round. Those packages just publish events, such as `SessionScheduled` or `MemberLeft`, without knowing whether anyone is listening. With Telegram off, nothing is.

## Four rules worth being able to explain

**1. Telegram calls never happen inside a database transaction.** Supabase gives us 3 connections, and a Telegram call can take up to 60 seconds. Holding a connection that long could stall the whole app. That's why `ChatStore` and `SessionChatStore` hold short transactions, while `GroupChatService` and `SessionChatService` are not transactional and call Telegram between them.

**2. Events run after the commit, on a background thread.** `ChatSyncListener` uses `@TransactionalEventListener` and `@Async`. If the save rolls back, Telegram never hears about it. If Telegram fails, the save still stands. The `telegramExecutor` runs two at a time with up to 200 waiting, so a slow Telegram never slows a web request. If even that fills up, the update is dropped and logged.

**3. Handlers read the current state instead of trusting the event.** Background events can arrive out of order, so a reply about a session cancelled a moment later must not go out as if it were still on. That's why `replyToCard` checks the session's status first, and the card is always rendered from current data.

**4. Group messages are ignored, never stored or logged.** As an admin, the bot receives every message in the chat. `UpdateClassifier` turns them into `BotEvent.Ignored`.

## The flows

### Setting up a chat
1. `GroupChatCard` calls `GroupChatController`, then `GroupChatService.startSetup`, `ChatStore.startSetup` and `TelegramLinkService.createGroupSetupLink`. This returns a one-time `t.me` link.
2. The leader presses Start. The poller, classifier and router reach `ChatBotHandler.openSetup`, which sends two buttons: create a group or use an existing one.
3. The leader picks a chat. `ChatBotHandler.chatChosen` reads it from Telegram, and checks it isn't linked to another study group, **before** using up the link, so a failure or a wrong choice can be retried. Telegram can send the old id of a group it has just upgraded, so `migrate_to_chat_id` is followed.
4. `ChatStore.saveLinkedChat` saves it, and only then is the link used up. If the bot has the rights it needs, `GroupChatService.finishSetup` sets permissions, the title and the description, and pins a welcome message.
5. If rights are missing, the chat stays `NEEDS_ATTENTION`. A later `my_chat_member` update, or the leader's "check again" button, reaches `finishSetup` once they're fixed. `GroupChat.setupFinishedAt` makes setup run only once, so losing and regaining rights later doesn't rename the chat or pin another message.

### Joining the chat
1. `GroupChatCard` asks for an invite through `GroupChatService.invite`. That creates a personal link with `creates_join_request=true`, so Telegram asks the bot before letting anyone in.
2. The student taps it, and `ChatBotHandler.joinRequested` calls `ChatStore.decideJoin`, which calls `ChatMembership.decideJoin`.
3. `JoinDecision` is one of `Approve`, `Decline` or `Ignore`. The rules are on the entity, so `ChatMembershipTest` can test every case without Spring.
4. On approval the bot saves who joined **first**, then approves. If saving fails, nobody has been let in. If Telegram refuses the approval, the row is reset and the student is told to get a new invite. Then it revokes the link and sends a welcome message. Joining deliberately never creates a `TelegramAccount`, so someone holding a forwarded invite can't take over a student's private messages.

### Leaving the study group
1. `MembershipService` publishes `MemberLeft`.
2. In the same transaction, `studysession/GroupEventListener` removes their answers to future sessions.
3. After the commit, `ChatSyncListener` calls `GroupChatService.onMemberLeft`, which revokes the invite and removes them from the chat. Banning is how a bot removes someone, and an unban straight after lets them be invited again later.
4. Telegram refuses to remove a chat's owner. That becomes `REMOVAL_FAILED`, and the leader sees a warning. A chat upgraded since the id was saved is followed to its new id, so that doesn't count as a refusal.

### The session card
This is the flow worth knowing best. Each session has one message in the chat, its card, and **one class changes it**: `SessionCardRefresher`.

1. Anything that affects a card only marks the session dirty: scheduling, changing or cancelling it, someone answering, a member joining or leaving. `ChatSyncListener` does this straight after the commit. It's just adding an id to a set in memory.
2. Every 3 seconds, one thread takes each dirty session, reads its **current** state and renders the text with `SessionMessages.card`.
3. No message yet and the session is still to come: it posts one, and `recordPost` saves the message id on `SessionChatCard`. Otherwise it edits the message. Editing never notifies anyone.
4. It saves the phase it just showed, then `syncPins` makes sure the soonest upcoming session is the pinned one.

Why it's built this way:
- **Bursts merge.** Five presses inside 3 seconds become one edit, which stays inside Telegram's limit of about 20 messages a minute per group.
- **No overwriting.** With one thread rendering current data, an older edit can't replace a newer one, so rule 3 above needs no extra code here.
- **Failures sort themselves out.** "Message is not modified" counts as success. "Message to edit not found" means someone deleted the card, so it's forgotten and posted again. A temporary failure, or a rate limit, pauses the refresher and tries again. A restart loses at most a few seconds of marks, and the next change or the sweep fixes that.

The phases are in `CardPhase`: `OPEN` has buttons, `STARTED` and `ENDED` don't, and `CANCELLED` is short.

### What happens when a session changes
`StudySession.update` returns a `SessionChange`, so the service knows what actually happened:
- **`NONE`:** nothing is saved or published, so pressing Save without changes does nothing at all.
- **`DETAILS`, including a new end time:** the card updates, quietly.
- **`START_MOVED`:** answers are cleared, because they were for the old time. The card updates, and `SessionChatService.startMoved` sends one reply asking people to answer again. The reminder can go out again for the new time.

Cancelling shows on the card, removes the buttons, unpins it and sends one short reply. A leader can cancel until an hour after the session ends. In that last hour the card still changes, but no reply goes out, because a message about a finished session would only disturb people.

### Pressing Going or Can't
1. The button carries `rsvp:<session id>:G`. Telegram allows 64 bytes, and this is 43.
2. The poller, classifier and router reach `SessionChatService.buttonPressed`, and `RsvpPress.parse` reads the data.
3. `SessionChatStore.responderFor` works out which student this is: first who they joined the chat as, then whoever connected that Telegram account. Nobody found means a pop-up, and nothing is saved.
4. `StudySessionService.respond` saves it, with the same rules as the app, and publishes `SessionAnswered`. So answering in the app updates the card the same way.
5. `answerButton` shows a small notice to the person who pressed. Any unexpected error still gets an answer, or the button would keep spinning.

### The sweep and reminders
Every 60 seconds `SessionCardJob` calls `SessionChatService.sweep`. This is the single answer to "how does the bot do things on time?". It only runs on the instance polling for updates, because a token can only have one.
1. `SessionChatStore.cardsToRefresh` marks two kinds of session dirty: upcoming ones with no card, which covers failed posts and chats linked late, and ones whose saved phase no longer matches the clock, which is how buttons go at the start and the pin goes at the end.
2. `sendDueReminders` finds sessions starting within the lead time, in a working chat, with a card and no reminder sent. It skips any whose card was posted or moved after the window opened, because the chat has only just heard about it.
3. Each reminder is marked as sent first, then sent as a reply to the card. If Telegram refuses, the mark is cleared and the next run tries again. If the app crashes in between, one reminder is lost, which is better than sending it twice.

### Nudges and the leader warning
The sweep's last step is `sendDueNudges`. It's the one thing the bot does to get answers rather than report them.
1. `SessionChatCardRepository.findAwaitingNudge` finds posted cards of scheduled sessions in working chats, with no nudge sent, starting within the lead time.
2. `NudgeRule.isDue` decides whether now is right. It's a plain function taking `now`, so its edge cases are tested without Spring. The window opens `TELEGRAM_NUDGE_LEAD` before the start and closes 3 hours before it. Nothing goes out outside 08:00 to 22:00 in `APP_TIME_ZONE`, so a nudge due overnight waits for the morning. A session announced after the window opened is skipped, because the chat has only just been asked.
3. The session is marked first, as with reminders. `silentInChat` lists members with no answer whom the bot can tag. If there are any, one reply to the card tags them. People not in the chat can't be tagged, but the card still names them.
4. `warnLeaderIfFewAreGoing` then privately tells the leader if fewer than two are going, in a group of at least two. It runs even when nobody was tagged, because everyone answering "Can't" is when the leader most needs to know.

If the reply fails, the mark is cleared and the next run tries again. The warning comes after the reply, so a retry can't warn twice. A failed warning is only logged, because the likely cause is a leader who blocked the bot, and retrying would send the nudge again. Moving the start clears the mark, so the new time gets its own nudge.

### When a video chat starts
Telegram has no way for a bot to start a call or invite anyone to one. A chat admin starts it, Telegram sends a `video_chat_started` message, and `SessionChatService.videoChatStarted` tags the others going, if a video chat session is due within 30 minutes or is running.

## Where the data lives

| Table | One row means |
|---|---|
| `telegram_account` | A student connected a Telegram account, per bot |
| `telegram_link_token` | A one-time link. Stored hashed, 10 minutes |
| `group_chat` | A study group's chat, per bot |
| `chat_membership` | One student's place in that chat |
| `session_chat_card` | One session's card: its message, when it was announced, whether its reminder and nudge went out, the phase it shows and whether it's pinned |

A Telegram account can only be one student per chat: `chat_membership` has a unique constraint on chat and Telegram user. Several lookups rely on that to find one row.

Foreign keys to `student` use `@OnDelete(CASCADE)` so deleting an account still works. Each teammate has their own bot, so queries filter on `bot_id`, and a chat owned by another bot shows `MANAGED_BY_OTHER_BOT`.

## Likely viva questions

**Why write your own Telegram client instead of using a library?**
Java Telegram libraries weren't compatible with Spring Boot 4 and Jackson 3 when we chose, and we use few enough methods that a small `RestClient` wrapper is simple and avoids a new dependency.

**Why long polling and not a webhook?**
A webhook needs a public HTTPS address. Polling works from a laptop, which suits a team where everyone runs their own bot.

**Which OOP ideas does this show?**
Polymorphism through `TelegramClient`, with two implementations chosen by configuration. Sealed types, `BotEvent` and `JoinDecision`, where the compiler checks every case. Rules kept on entities, as in `ChatMembership.decideJoin` and `StudySession.schedule`. Loose coupling through events.

**How does a Telegram user become a student?**
Two ways, on purpose: through a personal invite, stored on `ChatMembership`, or by connecting their account, stored as a `TelegramAccount`. Only connecting allows private messages.

**Why is the reminder marked as sent before it's sent?**
So that a crash can't cause a duplicate. The cost is that a crash at exactly the wrong moment loses one reminder.

**Why edit one message rather than post updates?**
It gives the chat something to look at without adding noise. Each session sends at most a few notifying messages, and the card always shows the current answer in one place.

**Why a 3-second delay rather than updating at once?**
It merges bursts and keeps edits on one thread, so they stay in order and inside Telegram's limits. Nobody notices 3 seconds, because the person pressing gets an instant notice.

**Why does moving the start clear answers, but a new end time doesn't?**
Someone who said yes to Wednesday hasn't said yes to Friday. A session running 30 minutes longer doesn't change whether they can come.

**Why is `SessionChatCard` separate from `StudySession`?**
A session shouldn't know Telegram exists, and the app must work with Telegram off. Keeping that state in `telegram/chat/` keeps the dependency pointing one way.

**What if one Telegram account is used for two students?**
It's refused with a clear message, and the database has a unique constraint as a backstop. Without that, lookups by Telegram user find two rows and throw, which would break that person's buttons and joins.

**Why does approving a join save first?**
Because the two failures aren't equal. Saving first and failing leaves nobody in the chat. Approving first and failing to save leaves someone in the chat with no record, whom StudyBuddy could never remove.

**What happens if Telegram is down?**
The app carries on and the failure is logged. Cards and reminders try again by themselves. Only the short replies about a moved or cancelled session are lost, and the card still shows the change.

**Why does a nudge tag only some people?** Telling everyone trains people to mute the chat. Only those with no answer have anything to do, and only people the bot knows are in the chat can be tagged.

**Why is the nudge rule a separate class?** Time rules have awkward edges: quiet hours, a window opening overnight, a session moved late. As a plain function taking `now`, each edge gets a fast test with no database or clock.

**Why does the leader get a private message rather than a chat post?** It lets them act before the group sees a session looking thin. A bot can only message someone who has started it. Connecting an account or setting up the chat both do that, but someone made leader later may have done neither, so their warning fails and is only logged.

## Changes

### Moving reminder state out of `StudySession`
**Before:** `StudySession` held `reminderClaimedAt` and `reminderSentAt`, and two repository queries claimed a reminder so several instances wouldn't both send it.

**After:** `SessionChatCard` holds that state, and the job only runs on the polling instance. There's one sender, so no claim is needed.

**Why:** a session no longer carries Telegram fields, and about 30 lines of concurrency handling are gone. The README already says to run one polling instance per token, so the claim was guarding against something unsupported. Chat posts look the same.

### Reliability fixes
Each of these was a real way for something to go wrong, found by reading the code.

| Problem | Fix |
|---|---|
| One Telegram account on two students made two rows, and every lookup for that account then threw | A clear `409` in `invite`, and a unique constraint |
| A third background task blocked the web request that started it | A `ThreadPoolTaskExecutor` with a queue |
| "Choose another chat" appeared after the link had been used up and the buttons removed | The check comes first, and the link and buttons stay |
| Linking an existing group renamed it and reset permissions without saying so | The setup message now says so |
| Getting rights back re-ran setup: another rename and another pinned message | `setupFinishedAt` |
| An unexpected error left a pressed button spinning | Every press gets an answer |
| After "Not you?" with a failed new invite, the card showed nothing | The card handles `REMOVED` and reloads on failure |
| After 7 days the Join button was a dead link | The card fetches a new invite when the old one has expired |
| A failed save after approval left someone in the chat with no record | Save first, and undo if Telegram refuses |
| A removal in an upgraded group wrongly reported failure | `kick` follows the new id |
| The app kept the chat's original title after the bot renamed it | The new title is saved. A rename by a person in Telegram still isn't noticed |
| A group closed during the 10-minute setup window could still be linked | The link stops working when the group closes |

**Clean-up:** `ChatSender` now holds sending and following upgraded chats, so session code no longer depends on `GroupChatService`. Group messages no longer carry a `RemoveKeyboard` placeholder, because the plain `sendMessage` now returns the message id.

**Left as it is on purpose:** `TelegramAccount.canMessage` is always true, but removing it means changing the API and dropping a column. The test-only constructors on `Update` would mean rewriting about 30 test lines for little benefit. `ChatStore` stays one class, because splitting it wouldn't make any single flow easier to follow.

### The live card
**Before:** every change posted a new message with live buttons, the chat couldn't see who was coming, and a save with no changes still posted.

**After:** one card per session, updated in place, as described above.

**Known limits:** a deleted account's name stays on a card until its next change. Sessions with old-style posts turn into cards the first time the sweep sees them, but older posts of the same session stay as they were, and their buttons still work.

### Nudges and the leader warning
**Before:** the card showed who hadn't answered, but nothing asked them, and a leader only found out about low turnout by looking.

**After:** one tagged reply per session, about 24 hours before, and a private warning to the leader when fewer than two are going.

**Known limits:** the check runs once, so someone joining the chat afterwards isn't nudged, and answers changing afterwards don't produce a new warning. A session created inside the window gets neither the nudge nor the warning. If the app crashes between marking and sending, that session's nudge is lost, which is better than sending it twice. I found no official limit on how many people one message can notify, so tagging a whole large group needs checking with real accounts.

### The session and chat screens
**Before:** a session row could show up to six equal buttons, the next session looked like every other, and the form asked for an end time and a location even when it could work them out.

**After:**
- `SessionList.vue` shows Join, one Going or Can't choice, and a menu holding Add to calendar, Change and Cancel. `nextSessionBadge` in `utils/sessions.js` picks the session to highlight.
- `SessionFormModal.vue` asks "How will people join?" when the group has a working chat, and hides Location for online sessions unless one is already set. `endAfterStartChange` in `utils/calendar.js` fills in the end time: a new session gets two hours, and moving a session keeps its length.
- `GroupChatCard.vue` has shorter wording, numbered setup steps, and one summary of what the bot does.

Both helpers are plain functions, so they have fast tests with no browser. No API changed, so the README contract and the mock stay as they were.

**Replaced later:** the Starts and Ends fields and much of this wording, in the next section.

### Cancelling after the start
**Before:** `StudySession.cancel` refused once a session had started, and the row's menu disappeared with it. A meeting that fell through at the last minute stayed on record as if it had happened.

**After:** the last moment to cancel is the session's end time plus one hour, and a cancel at exactly that moment still counts.
- `cancel` checks that the caller leads the group, that the session isn't already cancelled, and then the time. `update` and `respond` still go through `requireChangeable`, so a started session can't be moved or answered.
- `SessionChatService.cancelled` replies in the chat only until the session's end time. In the last hour, only the card changes.
- `SessionList.vue` keeps Cancel in a leader's menu until the limit, including under Past. Change goes at the start, and calendar options at the end.

No schema changed, so there's nothing to apply on Supabase.

**Why allow it after the end at all, when Calendly, Meetup and Cal.com block it?** A leader often only knows a meeting fell through once its time has passed, and the group should be able to see that. The limit keeps old sessions as they were, so the list stays a fair record.

**Why is the limit in two places?** The backend's `StudySession.CANCEL_BUFFER` is the actual rule. `isTooLateToCancel` in `utils/sessions.js` only decides whether to show the option. If a page stays open past the limit, the leader gets the backend's refusal as a message.

### The date and time fields, and the wording
**Before:** two browser `datetime-local` fields, which look different in every browser and need a date and time picked twice. The subtext explained how the system works, such as "Bots can't start a call themselves."

**After:**
- `SessionTimeFields.vue` shows one calendar for the date, then Starts and Ends as lists in 15-minute steps. Ends shows each length, such as "9:00 pm (2 hrs)", and says "next day" past midnight, so a second date isn't needed.
- The logic is in `startTimeOptions`, `endTimeOptions` and `defaultSessionTimes` in `utils/calendar.js`, as plain functions with tests. The component only displays them, and `SessionFormModal.vue` still sends the same API values.
- Wording follows one test: add subtext only if the label isn't enough, say what the person does rather than how the system works, and leave out limits they can't act on.
- If a group has no chat, the form mentions linking one, because otherwise nobody would discover the Telegram option.

**Why a list for the time, rather than a clock or a scroller?** Study sessions start at round times, so a list is one tap, and on a phone a `<select>` opens the phone's own picker. A time off the grid, such as 19:07, stays listed, so opening the form never changes it.

**Why only use vue-datepicker for the date?** Its time controls are arrows and grids, not a list. That means it adds a dependency for the calendar alone. In return, we don't have to build or test a calendar's keyboard support ourselves.

**Details worth knowing:**
- Its stylesheet sets its own theme, so `SessionTimeFields.vue` points its CSS variables at the app's colours.
- The calendar stops Escape reaching the Bootstrap modal while it's open, so Escape closes only the calendar. When it closes, focus goes to the wrapper around the date field, because the library drops it otherwise and focusing its input reopens the calendar.
- It has no API impact, so the README contract and the mock didn't change.
