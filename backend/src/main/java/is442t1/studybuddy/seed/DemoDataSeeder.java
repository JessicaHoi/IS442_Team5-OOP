package is442t1.studybuddy.seed;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import is442t1.studybuddy.admin.SystemAdministrator;
import is442t1.studybuddy.common.web.TimeSlotDto;
import is442t1.studybuddy.connection.MatchRequest;
import is442t1.studybuddy.connection.MatchRequestRepository;
import is442t1.studybuddy.connection.StudyBuddyConnection;
import is442t1.studybuddy.connection.StudyBuddyConnectionRepository;
import is442t1.studybuddy.course.Course;
import is442t1.studybuddy.course.CourseRepository;
import is442t1.studybuddy.group.GroupDetails;
import is442t1.studybuddy.group.StudyGroup;
import is442t1.studybuddy.group.StudyGroupLeader;
import is442t1.studybuddy.group.StudyGroupRepository;
import is442t1.studybuddy.model.UserRepository;
import is442t1.studybuddy.model.enums.GroupStatus;
import is442t1.studybuddy.model.enums.RequestStatus;
import is442t1.studybuddy.student.Student;
import is442t1.studybuddy.student.StudentRepository;
import is442t1.studybuddy.student.StudyPreferenceDto;

/**
 * Loads the demo data (10 courses, 50 students, groups, connections) when the
 * database has no accounts yet. Turned off with {@code studybuddy.seed.enabled=false}.
 */
@Component
@ConditionalOnProperty(prefix = "studybuddy.seed", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final SeedProperties properties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final StudyGroupRepository groupRepository;
    private final MatchRequestRepository matchRequestRepository;
    private final StudyBuddyConnectionRepository connectionRepository;

    public DemoDataSeeder(SeedProperties properties, ResourceLoader resourceLoader, ObjectMapper objectMapper,
            PasswordEncoder passwordEncoder, UserRepository userRepository, CourseRepository courseRepository,
            StudentRepository studentRepository, StudyGroupRepository groupRepository,
            MatchRequestRepository matchRequestRepository, StudyBuddyConnectionRepository connectionRepository) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
        this.matchRequestRepository = matchRequestRepository;
        this.connectionRepository = connectionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (userRepository.count() > 0) {
            LOG.info("Database already has accounts; skipping demo data.");
            return;
        }
        SeedData data = readSeedData();
        // Hash once: every demo account shares the same password, and BCrypt is slow by design.
        String passwordHash = passwordEncoder.encode(properties.demoPassword());

        Map<String, Course> courses = seedCourses(data);
        seedAdministrators(data, passwordHash);
        Map<String, Student> students = seedStudents(data, courses, passwordHash);
        seedConnections(data, students);
        seedGroups(data, courses, students);
        LOG.info("Loaded demo data: {} courses, {} students, {} groups.",
                courses.size(), students.size(), data.groups().size());
    }

    private SeedData readSeedData() throws IOException {
        try (InputStream input = resourceLoader.getResource(properties.location()).getInputStream()) {
            return objectMapper.readValue(input, SeedData.class);
        }
    }

    private Map<String, Course> seedCourses(SeedData data) {
        return data.courses().stream()
                .map(seed -> {
                    Course course = new Course();
                    course.setCourseCode(seed.code());
                    course.setCourseName(seed.name());
                    course.setSchool(seed.school());
                    return courseRepository.save(course);
                })
                .collect(Collectors.toMap(Course::getCourseCode, Function.identity()));
    }

    private void seedAdministrators(SeedData data, String passwordHash) {
        for (SeedData.AdministratorSeed seed : data.administrators()) {
            SystemAdministrator administrator = new SystemAdministrator();
            administrator.setEmail(seed.email());
            administrator.setPassword(passwordHash);
            userRepository.save(administrator);
        }
    }

    /** Students who lead a group are created as {@link StudyGroupLeader}s straight away. */
    private Map<String, Student> seedStudents(SeedData data, Map<String, Course> courses, String passwordHash) {
        Set<String> leaderKeys = data.groups().stream().map(SeedData.GroupSeed::leaderKey).collect(Collectors.toSet());
        Map<String, Student> students = new HashMap<>();
        for (SeedData.StudentSeed seed : data.students()) {
            Student student = leaderKeys.contains(seed.key()) ? new StudyGroupLeader() : new Student();
            student.setEmail(seed.email());
            student.setPassword(passwordHash);
            student.setName(seed.name());
            student.setSchool(seed.school());
            student.setProgram(seed.programme());
            student.setYearOfStudy(seed.yearOfStudy());
            student.setContactNum(seed.contactNumber());
            student.replaceCoursesTaken(seed.courses().stream().map(courses::get).toList());
            for (StudyPreferenceDto preference : seed.preferences()) {
                student.saveStudyPreference(courses.get(preference.course()), preference.meetingMode(),
                        preference.groupFormat(), preference.goals(),
                        TimeSlotDto.toTimeSlots(preference.availability()));
            }
            students.put(seed.key(), studentRepository.save(student));
        }
        return students;
    }

    /** Accepted connections get both the original request and the active connection. */
    private void seedConnections(SeedData data, Map<String, Student> students) {
        for (SeedData.ConnectionSeed seed : data.connections()) {
            Student sender = students.get(seed.fromKey());
            Student receiver = students.get(seed.toKey());
            MatchRequest request = new MatchRequest();
            request.setSender(sender);
            request.setReceiver(receiver);
            request.setMessage(seed.message());
            request.setStatus(seed.status());
            matchRequestRepository.save(request);

            if (seed.status() == RequestStatus.ACCEPTED) {
                StudyBuddyConnection connection = new StudyBuddyConnection();
                connection.setRequester(sender);
                connection.setRecipient(receiver);
                connectionRepository.save(connection);
            }
        }
    }

    /** Groups are built through the same domain methods the API uses, so every rule still applies. */
    private void seedGroups(SeedData data, Map<String, Course> courses, Map<String, Student> students) {
        for (SeedData.GroupSeed seed : data.groups()) {
            StudyGroupLeader leader = (StudyGroupLeader) students.get(seed.leaderKey());
            GroupDetails details = new GroupDetails(seed.name(), seed.description(), seed.goals(), seed.meetingMode(),
                    TimeSlotDto.toTimeSlots(seed.availability()), seed.maxSize());
            StudyGroup group = leader.createGroup(courses.get(seed.courseCode()), details);

            for (String memberKey : seed.memberKeys()) {
                leader.acceptMember(group.requestToJoin(students.get(memberKey), ""));
            }
            for (SeedData.JoinRequestSeed request : seed.joinRequests()) {
                group.requestToJoin(students.get(request.studentKey()), request.message());
            }
            if (seed.status() == GroupStatus.CLOSED) {
                leader.closeGroup(group);
            }
            groupRepository.save(group);
        }
    }
}
