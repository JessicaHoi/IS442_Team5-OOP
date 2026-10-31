package is442t1.studybuddy.admin;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.auth.SessionTokenService;
import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.common.exception.PermissionDeniedException;
import is442t1.studybuddy.common.exception.ResourceNotFoundException;
import is442t1.studybuddy.common.exception.ValidationException;
import is442t1.studybuddy.connection.MatchRequestRepository;
import is442t1.studybuddy.connection.StudyBuddyConnectionRepository;
import is442t1.studybuddy.group.MembershipRequestRepository;
import is442t1.studybuddy.group.StudyGroup;
import is442t1.studybuddy.group.GroupLeadershipService;
import is442t1.studybuddy.group.LedGroupResponse;
import is442t1.studybuddy.group.StudyGroupRepository;
import is442t1.studybuddy.model.AccountUsageCount;
import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.UserRepository;
import is442t1.studybuddy.model.enums.AccountStatus;
import is442t1.studybuddy.model.enums.Role;
import is442t1.studybuddy.student.Student;

@Service
@Transactional(readOnly = true)
public class UserAccountService {

    private final UserRepository userRepository;
    private final StudyBuddyConnectionRepository connectionRepository;
    private final StudyGroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService tokenService;
    private final GroupLeadershipService leadershipService;
    private final MembershipRequestRepository membershipRequestRepository;
    private final MatchRequestRepository matchRequestRepository;
    private final ApplicationEventPublisher events;

    public UserAccountService(UserRepository userRepository, StudyBuddyConnectionRepository connectionRepository,
            StudyGroupRepository groupRepository, PasswordEncoder passwordEncoder, SessionTokenService tokenService,
            GroupLeadershipService leadershipService, MembershipRequestRepository membershipRequestRepository,
            MatchRequestRepository matchRequestRepository,
            ApplicationEventPublisher events) {
        this.userRepository = userRepository;
        this.connectionRepository = connectionRepository;
        this.groupRepository = groupRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.leadershipService = leadershipService;
        this.membershipRequestRepository = membershipRequestRepository;
        this.matchRequestRepository = matchRequestRepository;
        this.events = events;
    }

    public List<UserAccountResponse> listAccounts() {
        Map<UUID, Long> connections = countsByUser(connectionRepository.countActiveByStudent());
        Map<UUID, Long> groups = countsByUser(groupRepository.countMembershipsByStudent());
        Map<UUID, Long> leaderships = countsByUser(groupRepository.countLeadershipsByStudent());
        return userRepository.findAll(Sort.by("email")).stream()
                .map(user -> UserAccountResponse.from(user, connections.getOrDefault(user.getId(), 0L),
                        groups.getOrDefault(user.getId(), 0L), leaderships.getOrDefault(user.getId(), 0L)))
                .toList();
    }

    private static Map<UUID, Long> countsByUser(List<AccountUsageCount> counts) {
        return counts.stream().collect(Collectors.toMap(AccountUsageCount::getUserId, AccountUsageCount::getUsageCount));
    }

    @Transactional
    public UserAccountResponse createAccount(UUID callerId, CreateUserRequest request) {
        requireActiveAdministrator(callerId);
        requireAvailableEmail(request.email(), null);
        validatePassword(request.password());
        User user = request.role() == Role.STUDENT ? new Student() : new SystemAdministrator();
        setName(user, request.name());
        user.setEmail(request.email());
        user.setStatus(request.status());
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.saveAndFlush(user);
        return UserAccountResponse.from(user, 0, 0, 0);
    }

    @Transactional
    public UserAccountResponse updateAccount(UUID callerId, UUID userId, UpdateUserRequest request) {
        requireActiveAdministrator(callerId);
        User user = requireAccountForUpdate(userId);
        if (request.role() != user.getRole()) {
            throw new ValidationException("Account roles cannot change after creation.");
        }
        if (callerId.equals(userId) && request.status() != AccountStatus.ACTIVE) {
            throw new ValidationException("You cannot suspend your own account.");
        }
        requireAvailableEmail(request.email(), userId);
        boolean changePassword = request.password() != null && !request.password().isEmpty();
        if (changePassword) {
            validatePassword(request.password());
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        setName(user, request.name());
        user.setEmail(request.email());
        user.setStatus(request.status());
        userRepository.flush();
        if (changePassword || !user.isActive()) {
            tokenService.revokeAllForUser(userId);
        }
        return UserAccountResponse.from(user, connectionRepository.findActiveForStudent(userId).size(),
                groupRepository.findByMemberId(userId).size(), groupRepository.countByLeader_Id(userId));
    }

    private void requireActiveAdministrator(UUID callerId) {
        List<String> administrators = userRepository.lockAdministratorAccounts();
        User caller = userRepository.findById(callerId).orElse(null);
        if (!administrators.contains(callerId.toString()) || caller == null || !caller.isActive()) {
            throw new PermissionDeniedException("An active administrator account is required.");
        }
    }

    public List<LedGroupResponse> listLedGroups(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User account not found.");
        }
        return groupRepository.findByLeader_Id(userId).stream().map(LedGroupResponse::from).toList();
    }

    @Transactional
    public LedGroupResponse transferLeadership(UUID callerId, UUID userId, UUID groupId, UUID successorId) {
        requireActiveAdministrator(callerId);
        return leadershipService.transfer(userId, groupId, successorId, true);
    }

    @Transactional
    public void deleteAccount(UUID callerId, UUID userId) {
        requireActiveAdministrator(callerId);
        if (callerId.equals(userId)) {
            throw new ValidationException("You cannot delete your own account.");
        }
        User user = requireAccountForUpdate(userId);
        if (groupRepository.existsByLeader_Id(userId)) {
            throw new BusinessRuleException("Transfer leadership of every group before deleting this account.");
        }
        if (user instanceof Student) {
            events.publishEvent(new AccountDeleting(userId));
            Stream.concat(groupRepository.findByMemberId(userId).stream(),
                    membershipRequestRepository.findByStudent_Id(userId).stream().map(request -> request.getStudyGroup()))
                    .distinct().forEach(group -> group.removeDeletedAccount(userId));
            connectionRepository.deleteAll(connectionRepository.findAllForStudent(userId));
            matchRequestRepository.deleteAll(matchRequestRepository.findAllForStudent(userId));
            userRepository.flush();
        }
        userRepository.delete(user);
        userRepository.flush();
        tokenService.revokeAllForUser(userId);
    }

    private User requireAccountForUpdate(UUID userId) {
        userRepository.lockAccount(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));
    }

    private void requireAvailableEmail(String email, UUID existingId) {
        userRepository.findByEmailIgnoreCase(email).filter(user -> !user.getId().equals(existingId))
                .ifPresent(user -> {
                    throw new BusinessRuleException("Another account already uses this email address.");
                });
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters and must not be blank.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ValidationException("Password is too long. Use fewer characters.");
        }
    }

    private static void setName(User user, String name) {
        if (user instanceof Student student) {
            student.setName(name);
        } else if (user instanceof SystemAdministrator administrator) {
            administrator.setName(name);
        }
    }
}
