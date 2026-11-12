package is442t1.studybuddy.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.common.exception.ValidationException;
import is442t1.studybuddy.course.CourseService;
import is442t1.studybuddy.model.UserRepository;
import is442t1.studybuddy.student.Student;
import is442t1.studybuddy.student.StudentRepository;
import is442t1.studybuddy.student.TelegramHandle;

/** Registers student accounts and creates their initial sessions. */
@Service
@Transactional(readOnly = true)
public class RegistrationService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final CourseService courseService;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService tokenService;

    /**
     * Creates the registration service.
     *
     * @param userRepository the account repository
     * @param studentRepository the student repository
     * @param courseService the course catalogue service
     * @param passwordEncoder the password encoder
     * @param tokenService the session token service
     */
    public RegistrationService(UserRepository userRepository, StudentRepository studentRepository,
            CourseService courseService, PasswordEncoder passwordEncoder, SessionTokenService tokenService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.courseService = courseService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * Creates a student account and signs it in.
     *
     * @param request the registration details
     * @return the new session and student account details
     * @throws BusinessRuleException if an account already uses the email address
     * @throws ValidationException if the password or other submitted details are invalid
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new BusinessRuleException("Another account already uses this email address.");
        }
        validatePassword(request.password());
        request.newCourses().forEach(courseService::createCourse);

        Student student = new Student();
        student.setEmail(request.email());
        student.setPassword(passwordEncoder.encode(request.password()));
        student.setName(request.name());
        student.setSchool(request.school());
        student.setProgram(request.programme());
        student.setYearOfStudy(request.yearOfStudy());
        student.setContactNum(request.contactNumber());
        student.setTelegramHandle(TelegramHandle.normalize(request.telegramHandle()));
        student.replaceCoursesTaken(courseService.requireCourses(request.courses()));
        student.setLastLogin(Instant.now());
        studentRepository.saveAndFlush(student);

        String token = tokenService.issue(student);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    tokenService.revoke(token);
                }
            }
        });
        return new LoginResponse(token, SessionUser.from(student));
    }

    private static void validatePassword(String password) {
        if (password.isBlank() || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters and must not be blank.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ValidationException("Password is too long. Use fewer characters.");
        }
    }
}
