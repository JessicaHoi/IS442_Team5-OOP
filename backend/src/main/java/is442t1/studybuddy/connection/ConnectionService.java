package is442t1.studybuddy.connection;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.common.exception.PermissionDeniedException;
import is442t1.studybuddy.common.exception.ResourceNotFoundException;
import is442t1.studybuddy.common.exception.ValidationException;
import is442t1.studybuddy.model.enums.ConnectionStatus;
import is442t1.studybuddy.model.enums.RequestStatus;
import is442t1.studybuddy.student.Student;
import is442t1.studybuddy.student.StudentProfileMapper;
import is442t1.studybuddy.student.StudentProfileResponse;
import is442t1.studybuddy.student.StudentService;

/** Creates buddy requests, responds to them, and manages accepted connections. */
@Service
@Transactional
public class ConnectionService {

    private final MatchRequestRepository requestRepository;
    private final StudyBuddyConnectionRepository connectionRepository;
    private final StudentService studentService;
    private final BuddyStateService buddyStateService;
    private final StudentProfileMapper profileMapper;

    public ConnectionService(MatchRequestRepository requestRepository,
            StudyBuddyConnectionRepository connectionRepository, StudentService studentService,
            BuddyStateService buddyStateService, StudentProfileMapper profileMapper) {
        this.requestRepository = requestRepository;
        this.connectionRepository = connectionRepository;
        this.studentService = studentService;
        this.buddyStateService = buddyStateService;
        this.profileMapper = profileMapper;
    }

    public ConnectionResponse sendRequest(UUID senderId, CreateMatchRequest request) {
        if (senderId.equals(request.toStudentId())) {
            throw new ValidationException("You cannot send a request to yourself.");
        }
        Student sender = studentService.requireStudent(senderId);
        Student receiver = studentService.requireStudent(request.toStudentId());
        if (connectionRepository.findActiveBetween(senderId, receiver.getId()).isPresent()
                || !requestRepository.findPendingBetween(senderId, receiver.getId()).isEmpty()) {
            throw new BusinessRuleException("You already have a pending request or connection with this student.");
        }

        MatchRequest matchRequest = new MatchRequest();
        matchRequest.setSender(sender);
        matchRequest.setReceiver(receiver);
        matchRequest.setMessage(request.trimmedMessage());
        MatchRequest saved = requestRepository.save(matchRequest);
        return toRequestResponse(saved, senderId);
    }

    @Transactional(readOnly = true)
    public List<ConnectionResponse> list(UUID viewerId, String view) {
        return switch (view) {
            case "incoming" -> requestRepository.findByReceiver_IdAndStatus(viewerId, RequestStatus.PENDING)
                    .stream()
                    .sorted(Comparator.comparing(MatchRequest::getRequestDate).reversed())
                    .map(request -> toRequestResponse(request, viewerId))
                    .toList();
            case "sent" -> requestRepository.findBySender_IdAndStatus(viewerId, RequestStatus.PENDING)
                    .stream()
                    .sorted(Comparator.comparing(MatchRequest::getRequestDate).reversed())
                    .map(request -> toRequestResponse(request, viewerId))
                    .toList();
            case "active" -> connectionRepository.findActiveForStudent(viewerId).stream()
                    .sorted(Comparator.comparing(StudyBuddyConnection::getStartDate).reversed())
                    .map(connection -> toConnectionResponse(connection, viewerId))
                    .toList();
            default -> throw new ValidationException("Choose incoming, sent, or active connections.");
        };
    }

    public ConnectionResponse accept(UUID viewerId, UUID requestId) {
        MatchRequest request = requireRequest(requestId);
        requireRecipient(viewerId, request);
        requirePending(request);

        Instant acceptedAt = Instant.now();
        request.setStatus(RequestStatus.ACCEPTED);
        request.setRespondedAt(acceptedAt);
        requestRepository.save(request);

        StudyBuddyConnection connection = new StudyBuddyConnection();
        connection.setRequester(request.getSender());
        connection.setRecipient(request.getReceiver());
        connection.setStatus(ConnectionStatus.ACTIVE);
        connectionRepository.save(connection);
        return toRequestResponse(request, viewerId);
    }

    public ConnectionResponse decline(UUID viewerId, UUID requestId) {
        MatchRequest request = requireRequest(requestId);
        requireRecipient(viewerId, request);
        requirePending(request);
        request.setStatus(RequestStatus.REJECTED);
        request.setRespondedAt(Instant.now());
        requestRepository.save(request);
        return toRequestResponse(request, viewerId);
    }

    public void end(UUID viewerId, UUID connectionId) {
        StudyBuddyConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found."));
        if (!connection.getRequester().hasId(viewerId) && !connection.getRecipient().hasId(viewerId)) {
            throw new PermissionDeniedException("You do not have permission to do that.");
        }
        if (connection.getStatus() != ConnectionStatus.ACTIVE) {
            throw new BusinessRuleException("Only active connections can be ended.");
        }
        connection.setStatus(ConnectionStatus.ENDED);
    }

    private ConnectionResponse toRequestResponse(MatchRequest request, UUID viewerId) {
        boolean outgoing = request.getSender().hasId(viewerId);
        Student otherStudent = outgoing ? request.getReceiver() : request.getSender();
        return new ConnectionResponse(
                request.getId(),
                requestStatus(request.getStatus()),
                outgoing ? "OUTGOING" : "INCOMING",
                request.getMessage(),
                request.getRequestDate(),
                request.getRespondedAt(),
                profileMapper.toPublicProfile(otherStudent,
                        buddyStateService.stateBetween(viewerId, otherStudent.getId())));
    }

    private ConnectionResponse toConnectionResponse(StudyBuddyConnection connection, UUID viewerId) {
        boolean outgoing = connection.getRequester().hasId(viewerId);
        Student otherStudent = outgoing ? connection.getRecipient() : connection.getRequester();
        return new ConnectionResponse(
                connection.getId(),
                "ACCEPTED",
                outgoing ? "OUTGOING" : "INCOMING",
                null,
                connection.getStartDate(),
                connection.getStartDate(),
                profileMapper.toPublicProfile(otherStudent,
                        buddyStateService.stateBetween(viewerId, otherStudent.getId())));
    }

    private MatchRequest requireRequest(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found."));
    }

    private static void requireRecipient(UUID viewerId, MatchRequest request) {
        if (!request.getReceiver().hasId(viewerId)) {
            throw new PermissionDeniedException("Only the recipient can respond to this request.");
        }
    }

    private static void requirePending(MatchRequest request) {
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessRuleException("This request has already been answered.");
        }
    }

    private static String requestStatus(RequestStatus status) {
        return switch (status) {
            case PENDING -> "PENDING";
            case ACCEPTED -> "ACCEPTED";
            case REJECTED -> "DECLINED";
        };
    }
}
