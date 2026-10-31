package is442t1.studybuddy.telegram.chat;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.common.exception.PermissionDeniedException;
import is442t1.studybuddy.common.exception.ResourceNotFoundException;
import is442t1.studybuddy.group.StudyGroup;
import is442t1.studybuddy.group.StudyGroupRepository;
import is442t1.studybuddy.student.Student;
import is442t1.studybuddy.student.StudentRepository;
import is442t1.studybuddy.telegram.TelegramProperties;
import is442t1.studybuddy.telegram.link.TelegramLinkResponse;
import is442t1.studybuddy.telegram.link.TelegramLinkService;

/**
 * The database side of group chats, in short transactions. {@link GroupChatService} calls Telegram between these
 * methods, so a slow Telegram call never keeps one of the few database connections.
 */
@Service
@Transactional
public class ChatStore {

    /** What the Telegram side needs to know about a chat, safe to use outside a transaction. */
    record ChatRef(UUID id, UUID groupId, long chatId, String chatType, GroupChatStatus status, String groupName,
            String courseCode, String description, String missingRights) {

        static ChatRef of(GroupChat chat) {
            StudyGroup group = chat.getStudyGroup();
            return new ChatRef(chat.getId(), group.getId(), chat.getChatId(), chat.getChatType(), chat.getStatus(),
                    group.getGroupName(), group.getCourse().getCourseCode(), group.getDescription(), chat.getMissingRights());
        }

        boolean isSupergroup() {
            return "supergroup".equals(chatType);
        }

        boolean lacks(String right) {
            return Arrays.asList(missingRights.split(",")).contains(right);
        }
    }

    /** One student's position, read before asking Telegram for an invite. */
    record InviteState(ChatRef chat, ChatMemberState state, String inviteLink, Instant inviteExpiresAt, Long telegramUserId,
            String telegramName, Long linkedTelegramUserId, boolean inviteStillValid) {
    }

    /** A join request and what to do about it. */
    record JoinOutcome(JoinDecision decision, ChatRef chat, UUID studentId, String link) {
    }

    /** Someone to take out of a chat. */
    record Removal(UUID groupChatId, UUID studentId, long chatId, String chatType, Long telegramUserId, String inviteLink,
            ChatMemberState state) {
    }

    private final GroupChatRepository chatRepository;
    private final ChatMembershipRepository membershipRepository;
    private final StudyGroupRepository groupRepository;
    private final StudentRepository studentRepository;
    private final TelegramLinkService linkService;
    private final TelegramProperties properties;

    public ChatStore(GroupChatRepository chatRepository, ChatMembershipRepository membershipRepository,
            StudyGroupRepository groupRepository, StudentRepository studentRepository, TelegramLinkService linkService,
            TelegramProperties properties) {
        this.chatRepository = chatRepository;
        this.membershipRepository = membershipRepository;
        this.groupRepository = groupRepository;
        this.studentRepository = studentRepository;
        this.linkService = linkService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public GroupChatResponse view(UUID viewerId, UUID groupId) {
        StudyGroup group = requireMemberGroup(viewerId, groupId);
        Optional<GroupChat> found = chatRepository.findByStudyGroup_Id(groupId);
        if (found.isEmpty() || !properties.enabled()) {
            return GroupChatResponse.notLinked();
        }
        GroupChat chat = found.get();
        if (chat.getBotId() != properties.botId()) {
            return GroupChatResponse.managedByOtherBot();
        }
        boolean leader = group.isLedBy(viewerId);
        List<ChatMembership> memberships = membershipRepository.findByGroupChat_Id(chat.getId());
        GroupChatResponse.Me me = memberships.stream()
                .filter(membership -> membership.getStudent().getId().equals(viewerId))
                .findFirst()
                .map(ChatStore::toMe)
                .orElseGet(() -> new GroupChatResponse.Me(ChatMemberState.NOT_INVITED, null, null, null));
        List<GroupChatResponse.Member> members = leader ? group.getMembers().stream()
                .sorted(Comparator.comparing(Student::getName))
                .map(student -> toMember(student, memberships))
                .toList() : null;
        List<String> missing = leader ? Arrays.stream(chat.getMissingRights().split(",")).filter(name -> !name.isBlank()).toList() : null;
        return new GroupChatResponse(chat.getStatus().name(), chat.getTitle(), chat.getChatType(), missing,
                leader && !viewerId.equals(chat.getLinkedByStudentId()), me, members);
    }

    private static GroupChatResponse.Me toMe(ChatMembership membership) {
        return new GroupChatResponse.Me(membership.getState(), membership.getTelegramName(), membership.getInviteLink(),
                membership.getInviteExpiresAt());
    }

    private static GroupChatResponse.Member toMember(Student student, List<ChatMembership> memberships) {
        return memberships.stream()
                .filter(membership -> membership.getStudent().getId().equals(student.getId()))
                .findFirst()
                .map(membership -> new GroupChatResponse.Member(student.getId(), student.getName(), membership.getState(), membership.getTelegramName()))
                .orElseGet(() -> new GroupChatResponse.Member(student.getId(), student.getName(), ChatMemberState.NOT_INVITED, null));
    }

    /** Checks the leader may set up a chat and gives them the link to do it. */
    public TelegramLinkResponse startSetup(UUID leaderId, UUID groupId) {
        StudyGroup group = requireLedOpenGroup(leaderId, groupId);
        chatRepository.findByStudyGroup_Id(groupId).filter(chat -> chat.getStatus() != GroupChatStatus.DETACHED).ifPresent(chat -> {
            throw new BusinessRuleException("This group already has a chat.");
        });
        return linkService.createGroupSetupLink(group.getLeader(), group);
    }

    /**
     * Saves the chat a leader chose, replacing a chat the bot was removed from. Returns empty if it's already linked
     * to another study group.
     */
    public Optional<ChatRef> saveLinkedChat(UUID groupId, long chatId, String chatType, String title, long leaderTelegramUserId,
            String leaderName, ChatRights rights) {
        long botId = properties.botId();
        Optional<GroupChat> sameChat = chatRepository.findByBotIdAndChatId(botId, chatId);
        if (sameChat.isPresent() && !sameChat.get().getStudyGroup().getId().equals(groupId)) {
            return Optional.empty();
        }
        chatRepository.findByStudyGroup_Id(groupId).ifPresent(existing -> {
            chatRepository.delete(existing);
            // Hibernate runs inserts before deletes, which would break the unique constraint.
            chatRepository.flush();
        });
        StudyGroup group = groupRepository.findById(groupId).orElseThrow();
        Instant now = Instant.now();
        Student leader = group.getLeader();
        GroupChat chat = chatRepository.save(new GroupChat(group, botId, chatId, chatType, title, leader.getId(), rights, now));
        ChatMembership membership = new ChatMembership(chat, leader, now);
        membership.markJoined(leaderTelegramUserId, leaderName, now);
        membershipRepository.save(membership);
        return Optional.of(ChatRef.of(chat));
    }

    @Transactional(readOnly = true)
    public Optional<ChatRef> findByGroup(UUID groupId) {
        return chatRepository.findByStudyGroup_Id(groupId)
                .filter(chat -> chat.getBotId() == properties.botId())
                .map(ChatRef::of);
    }

    @Transactional(readOnly = true)
    public Optional<ChatRef> findByChat(long chatId) {
        return chatRepository.findByBotIdAndChatId(properties.botId(), chatId).map(ChatRef::of);
    }

    @Transactional(readOnly = true)
    public ChatRef requireLedChat(UUID leaderId, UUID groupId) {
        requireLeader(leaderId, groupId);
        return findByGroup(groupId).orElseThrow(() -> new BusinessRuleException("This group has no linked chat."));
    }

    public void migrate(long oldChatId, long newChatId) {
        chatRepository.findByBotIdAndChatId(properties.botId(), oldChatId).ifPresent(chat -> chat.migrateTo(newChatId));
    }

    /** Stores the bot's rights. Returns the chat if this made it usable, so setup can finish. */
    public Optional<ChatRef> updateRights(long chatId, ChatRights rights) {
        return chatRepository.findByBotIdAndChatId(properties.botId(), chatId)
                .filter(chat -> chat.applyRights(rights))
                .map(ChatRef::of);
    }

    public void markDetached(long chatId) {
        chatRepository.findByBotIdAndChatId(properties.botId(), chatId).ifPresent(GroupChat::markDetached);
    }

    public void delete(UUID groupChatId) {
        chatRepository.deleteById(groupChatId);
    }

    /** Checks the caller may have an invite, and says where they stand. */
    @Transactional(readOnly = true)
    public InviteState inviteState(UUID studentId, UUID groupId) {
        StudyGroup group = requireMemberGroup(studentId, groupId);
        Student student = studentRepository.findById(studentId).orElseThrow();
        if (!group.isOpen() || !student.isActive()) {
            throw new BusinessRuleException("Invites are only for active members of an open group.");
        }
        GroupChat chat = chatRepository.findByStudyGroup_Id(groupId)
                .filter(found -> found.getBotId() == properties.botId() && found.getStatus() == GroupChatStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException("This group's chat isn't available right now."));
        Long linked = linkService.linkedTelegramUserId(studentId).orElse(null);
        return membershipRepository.findByGroupChat_IdAndStudent_Id(chat.getId(), studentId)
                .map(membership -> new InviteState(ChatRef.of(chat), membership.getState(), membership.getInviteLink(),
                        membership.getInviteExpiresAt(), membership.getTelegramUserId(), membership.getTelegramName(), linked,
                        membership.hasValidInvite(Instant.now())))
                .orElseGet(() -> new InviteState(ChatRef.of(chat), ChatMemberState.NOT_INVITED, null, null, null, null, linked, false));
    }

    public void saveInvite(UUID groupChatId, UUID studentId, String link, String name, Instant expiresAt) {
        membershipFor(groupChatId, studentId).invite(link, name, expiresAt, Instant.now());
    }

    public void saveJoined(UUID groupChatId, UUID studentId, long telegramUserId, String telegramName) {
        membershipFor(groupChatId, studentId).markJoined(telegramUserId, telegramName, Instant.now());
    }

    public void saveRemoved(UUID groupChatId, UUID studentId) {
        membershipFor(groupChatId, studentId).markRemoved(Instant.now());
    }

    public void saveRemovalFailed(UUID groupChatId, UUID studentId) {
        membershipFor(groupChatId, studentId).markRemovalFailed(Instant.now());
    }

    private ChatMembership membershipFor(UUID groupChatId, UUID studentId) {
        return membershipRepository.findByGroupChat_IdAndStudent_Id(groupChatId, studentId).orElseGet(() -> {
            GroupChat chat = chatRepository.findById(groupChatId).orElseThrow();
            Student student = studentRepository.findById(studentId).orElseThrow();
            return membershipRepository.save(new ChatMembership(chat, student, Instant.now()));
        });
    }

    /** Works out what to do about a join request. Changes nothing. */
    @Transactional(readOnly = true)
    public JoinOutcome decideJoin(long chatId, String linkName, String link, long askingUserId) {
        Optional<GroupChat> foundChat = chatRepository.findByBotIdAndChatId(properties.botId(), chatId);
        if (foundChat.isEmpty()) {
            return new JoinOutcome(new JoinDecision.Ignore(), null, null, link);
        }
        GroupChat chat = foundChat.get();
        Optional<ChatMembership> found = Optional.ofNullable(linkName)
                .flatMap(name -> membershipRepository.findByGroupChat_IdAndInviteName(chat.getId(), name))
                .or(() -> Optional.ofNullable(link).flatMap(url -> membershipRepository.findByGroupChat_IdAndInviteLink(chat.getId(), url)));
        ChatRef ref = ChatRef.of(chat);
        if (found.isEmpty()) {
            // Ours by its name, but replaced since: refuse. Anything else belongs to the chat's human admins.
            return linkName != null && linkName.startsWith(GroupChatService.INVITE_NAME_PREFIX)
                    ? new JoinOutcome(new JoinDecision.Decline(ChatMembership.LINK_NOT_VALID), ref, null, link)
                    : new JoinOutcome(new JoinDecision.Ignore(), ref, null, link);
        }
        ChatMembership membership = found.get();
        Student student = membership.getStudent();
        UUID studentId = student.getId();
        StudyGroup group = chat.getStudyGroup();
        boolean activeMember = group.isOpen() && group.hasMember(studentId) && student.isActive();
        Long linked = linkService.linkedTelegramUserId(studentId).orElse(null);
        boolean usedByAnother = membershipRepository.findByGroupChat_IdAndTelegramUserId(chat.getId(), askingUserId)
                .filter(other -> !other.getStudent().getId().equals(studentId))
                .isPresent();
        return new JoinOutcome(membership.decideJoin(activeMember, linked, usedByAnother, askingUserId, Instant.now()), ref, studentId, link);
    }

    /** Follows someone joining or leaving in Telegram. */
    public void followChatMember(long chatId, long telegramUserId, boolean inChat) {
        chatRepository.findByBotIdAndChatId(properties.botId(), chatId)
                .flatMap(chat -> membershipRepository.findByGroupChat_IdAndTelegramUserId(chat.getId(), telegramUserId))
                .ifPresent(membership -> {
                    Instant now = Instant.now();
                    if (inChat && membership.getState() == ChatMemberState.LEFT) {
                        membership.markJoined(telegramUserId, membership.getTelegramName(), now);
                    } else if (!inChat && membership.getState() == ChatMemberState.JOINED) {
                        membership.markLeft(now);
                    } else if (!inChat && membership.getState() == ChatMemberState.REMOVAL_FAILED) {
                        membership.markRemoved(now);
                    }
                });
    }

    /** A student who has just joined the study group starts afresh in the chat. Returns the chat if it's usable. */
    public Optional<ChatRef> resetForNewMember(UUID groupId, UUID studentId) {
        return chatRepository.findByStudyGroup_Id(groupId)
                .filter(chat -> chat.getBotId() == properties.botId())
                .map(chat -> {
                    membershipRepository.findByGroupChat_IdAndStudent_Id(chat.getId(), studentId)
                            .ifPresent(membership -> membership.reset(Instant.now()));
                    return ChatRef.of(chat);
                });
    }

    /** Who to take out of this group's chat, if anyone, unless they've rejoined the study group since. */
    @Transactional(readOnly = true)
    public Optional<Removal> removalFor(UUID groupId, UUID studentId) {
        return chatRepository.findByStudyGroup_Id(groupId)
                .filter(chat -> chat.getBotId() == properties.botId())
                .filter(chat -> !chat.getStudyGroup().hasMember(studentId))
                .flatMap(chat -> membershipRepository.findByGroupChat_IdAndStudent_Id(chat.getId(), studentId))
                .map(ChatStore::toRemoval);
    }

    /** Everything needed to take a student out of every chat, read before their account is deleted. */
    @Transactional(readOnly = true)
    public List<Removal> removalsForDeletedAccount(UUID studentId) {
        return membershipRepository.findByStudent_Id(studentId).stream()
                .filter(membership -> membership.getGroupChat().getBotId() == properties.botId())
                .map(ChatStore::toRemoval)
                .toList();
    }

    private static Removal toRemoval(ChatMembership membership) {
        GroupChat chat = membership.getGroupChat();
        return new Removal(chat.getId(), membership.getStudent().getId(), chat.getChatId(), chat.getChatType(),
                membership.getTelegramUserId(), membership.getInviteLink(), membership.getState());
    }

    /** Closes the chat's record and returns the invite links still to revoke. */
    public List<String> closeChat(UUID groupChatId) {
        GroupChat chat = chatRepository.findById(groupChatId).orElseThrow();
        chat.markClosed();
        Instant now = Instant.now();
        List<ChatMembership> memberships = membershipRepository.findByGroupChat_Id(groupChatId);
        List<String> links = memberships.stream().map(ChatMembership::getInviteLink).filter(link -> link != null).toList();
        memberships.stream().filter(membership -> membership.getState() == ChatMemberState.INVITED)
                .forEach(membership -> membership.reset(now));
        return links;
    }

    /** The links to revoke before unlinking a chat. */
    @Transactional(readOnly = true)
    public List<String> activeLinks(UUID groupChatId) {
        return membershipRepository.findByGroupChat_Id(groupChatId).stream()
                .map(ChatMembership::getInviteLink).filter(link -> link != null).toList();
    }

    private StudyGroup requireMemberGroup(UUID studentId, UUID groupId) {
        StudyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Study group not found."));
        if (!group.hasMember(studentId)) {
            throw new PermissionDeniedException("Only group members can see the group chat.");
        }
        return group;
    }

    private StudyGroup requireLeader(UUID leaderId, UUID groupId) {
        StudyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Study group not found."));
        if (!group.isLedBy(leaderId)) {
            throw new PermissionDeniedException("Only the group leader can do that.");
        }
        return group;
    }

    private StudyGroup requireLedOpenGroup(UUID leaderId, UUID groupId) {
        StudyGroup group = requireLeader(leaderId, groupId);
        if (!group.isOpen()) {
            throw new BusinessRuleException("This group is closed.");
        }
        return group;
    }
}
