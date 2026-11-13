package is442t1.studybuddy.auth;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import is442t1.studybuddy.common.exception.BusinessRuleException;
import is442t1.studybuddy.course.CourseService;
import is442t1.studybuddy.model.UserRepository;
import is442t1.studybuddy.student.Student;
import is442t1.studybuddy.student.StudentRepository;
import is442t1.studybuddy.student.TelegramHandle;

/**
 * Creates active student accounts with a complete profile and optional new courses in one
 *     transaction. Returns a signed-in session and revokes the issued token if the transaction
 *     fails.
 */
@Service
@Transactional(readOnly = true)
public class RegistrationService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final CourseService courseService;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService tokenService;

    public RegistrationService(UserRepository userRepository, StudentRepository studentRepository,
            CourseService courseService, PasswordEncoder passwordEncoder, SessionTokenService tokenService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.courseService = courseService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new BusinessRuleException("Another account already uses this email address.");
        }
        PasswordPolicy.validate(request.password());
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
}
