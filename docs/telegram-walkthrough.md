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

Timer ──> SessionReminderJob ──> SessionChatService ──> Telegram
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

**2. Events run after the commit, on a background thread.** `ChatSyncListener` uses `@TransactionalEventListener` and `@Async`. If the save rolls back, Telegram never hears about it. If Telegram fails, the save still stands.

**3. Handlers read the current state instead of trusting the event.** Background events can arrive out of order, so a post for a session cancelled a moment later must not announce it as scheduled. That's why `postIf` checks the session's status first.

**4. Group messages are ignored, never stored or logged.** As an admin, the bot receives every message in the chat. `UpdateClassifier` turns them into `BotEvent.Ignored`.

## The flows

### Setting up a chat
1. `GroupChatCard` calls `GroupChatController`, then `GroupChatService.startSetup`, `ChatStore.startSetup` and `TelegramLinkService.createGroupSetupLink`. This returns a one-time `t.me` link.
2. The leader presses Start. The poller, classifier and router reach `ChatBotHandler.openSetup`, which sends two buttons: create a group or use an existing one.
3. The leader picks a chat. `ChatBotHandler.chatChosen` reads it from Telegram **before** using up the link, so a failure can be retried. Telegram can send the old id of a group it has just upgraded, so `migrate_to_chat_id` is followed.
4. `ChatStore.saveLinkedChat` saves it. If the bot has the rights it needs, `GroupChatService.finishSetup` sets permissions, the title and the description, and pins a welcome message.
5. If rights are missing, the chat stays `NEEDS_ATTENTION`. A later `my_chat_member` update, or the leader's "check again" button, reaches `finishSetup` once they're fixed.

### Joining the chat
1. `GroupChatCard` asks for an invite through `GroupChatService.invite`. That creates a personal link with `creates_join_request=true`, so Telegram asks the bot before letting anyone in.
2. The student taps it, and `ChatBotHandler.joinRequested` calls `ChatStore.decideJoin`, which calls `ChatMembership.decideJoin`.
3. `JoinDecision` is one of `Approve`, `Decline` or `Ignore`. The rules are on the entity, so `ChatMembershipTest` can test every case without Spring.
4. On approval the bot saves who joined, revokes the link and sends a welcome message. Joining deliberately never creates a `TelegramAccount`, so someone holding a forwarded invite can't take over a student's private messages.

### Leaving the study group
1. `MembershipService` publishes `MemberLeft`.
2. In the same transaction, `studysession/GroupEventListener` removes their answers to future sessions.
3. After the commit, `ChatSyncListener` calls `GroupChatService.onMemberLeft`, which revokes the invite and removes them from the chat. Banning is how a bot removes someone, and an unban straight after lets them be invited again later.
4. Telegram refuses to remove a chat's owner. That becomes `REMOVAL_FAILED`, and the leader sees a warning.

### Scheduling a session
1. `SessionList` calls `StudySessionController`, `StudySessionService.schedule` and `StudySession.schedule`, where the rules live. The service publishes `SessionScheduled`.
2. After the commit, `ChatSyncListener` calls `SessionChatService.postScheduled`.
3. It reads the session and checks for a working chat, then `SessionMessages` writes the text and `GroupChatService.send` posts it.
4. `SessionChatStore.recordPost` saves the message id and time on the session's `SessionChatCard`.

Changing or cancelling follows the same path.

### Pressing Going or Can't
1. The button carries `rsvp:<session id>:G`. Telegram allows 64 bytes, and this is 43.
2. The poller, classifier and router reach `SessionChatService.buttonPressed`, and `RsvpPress.parse` reads the data.
3. `SessionChatStore.responderFor` works out which student this is: first who they joined the chat as, then whoever connected that Telegram account. Nobody found means a pop-up, and nothing is saved.
4. `StudySessionService.respond` saves it, with the same rules as the app.
5. `answerButton` shows a small notice to the person who pressed.

### Sending reminders
1. Every 60 seconds `SessionReminderJob` calls `SessionChatService.sendDueReminders`. It only runs on the instance polling for updates, because a token can only have one.
2. `SessionChatStore.dueReminders` finds sessions starting within the lead time, in a working chat, with no reminder sent.
3. It skips any session announced after its reminder window opened, because the chat has only just heard about it.
4. Each reminder is marked as sent first, then sent. If Telegram refuses, the mark is cleared and the next run tries again. If the app crashes in between, one reminder is lost, which is better than sending it twice.

### When a video chat starts
Telegram has no way for a bot to start a call or invite anyone to one. A chat admin starts it, Telegram sends a `video_chat_started` message, and `SessionChatService.videoChatStarted` tags the others going, if a video chat session is due within 30 minutes or is running.

## Where the data lives

| Table | One row means |
|---|---|
| `telegram_account` | A student connected a Telegram account, per bot |
| `telegram_link_token` | A one-time link. Stored hashed, 10 minutes |
| `group_chat` | A study group's chat, per bot |
| `chat_membership` | One student's place in that chat |
| `session_chat_card` | What the chat has been told about one session: its message, when it was announced and whether its reminder went out |

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

**Why is `SessionChatCard` separate from `StudySession`?**
A session shouldn't know Telegram exists, and the app must work with Telegram off. Keeping that state in `telegram/chat/` keeps the dependency pointing one way.

**What happens if Telegram is down?**
The app carries on and the failure is logged. Reminders try again on the next run. Other posts are lost for now, which a later change fixes.

## Changes

### Moving reminder state out of `StudySession`
**Before:** `StudySession` held `reminderClaimedAt` and `reminderSentAt`, and two repository queries claimed a reminder so several instances wouldn't both send it.

**After:** `SessionChatCard` holds that state, and the job only runs on the polling instance. There's one sender, so no claim is needed.

**Why:** a session no longer carries Telegram fields, and about 30 lines of concurrency handling are gone. The README already says to run one polling instance per token, so the claim was guarding against something unsupported. Chat posts look the same.
