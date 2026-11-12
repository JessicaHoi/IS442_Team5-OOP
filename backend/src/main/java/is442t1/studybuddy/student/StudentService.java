package is442t1.studybuddy.student;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.exception.ResourceNotFoundException;
import is442t1.studybuddy.common.exception.ValidationException;
import is442t1.studybuddy.common.web.TimeSlotDto;
import is442t1.studybuddy.connection.BuddyStateService;
import is442t1.studybuddy.course.Course;
import is442t1.studybuddy.course.CourseService;

/** Profile and study-preference management for students. */
@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final CourseService courseService;
    private final BuddyStateService buddyStateService;
    private final StudentProfileMapper profileMapper;

    /**
     * Creates the student service.
     *
     * @param studentRepository the student repository
     * @param courseService the course catalogue service
     * @param buddyStateService the relationship lookup service
     * @param profileMapper the profile response mapper
     */
    public StudentService(StudentRepository studentRepository, CourseService courseService,
            BuddyStateService buddyStateService, StudentProfileMapper profileMapper) {
        this.studentRepository = studentRepository;
        this.courseService = courseService;
        this.buddyStateService = buddyStateService;
        this.profileMapper = profileMapper;
    }

    /**
     * Gets a student's own profile.
     *
     * @param studentId the student identifier
     * @return the student's profile
     */
    @Transactional(readOnly = true)
    public StudentProfileResponse getOwnProfile(UUID studentId) {
        return profileMapper.toOwnProfile(requireStudent(studentId));
    }

    /**
     * Gets a profile using the viewer's relationship to determine contact visibility.
     *
     * @param viewerId the viewing student's identifier
     * @param studentId the profile's student identifier
     * @return the visible profile
     */
    @Transactional(readOnly = true)
    public StudentProfileResponse getPublicProfile(UUID viewerId, UUID studentId) {
        if (viewerId.equals(studentId)) {
            return getOwnProfile(studentId);
        }
        Student student = requireStudent(studentId);
        return profileMapper.toPublicProfile(student, buddyStateService.stateBetween(viewerId, studentId));
    }

    /**
     * Replaces editable profile details and the selected courses.
     *
     * @param studentId the student identifier
     * @param request the replacement profile details
     * @return the updated profile
     */
    public StudentProfileResponse updateProfile(UUID studentId, UpdateProfileRequest request) {
        Student student = requireStudent(studentId);
        student.setName(request.name().trim());
        student.setSchool(request.school().trim());
        student.setProgram(request.programme().trim());
        student.setYearOfStudy(request.yearOfStudy());
        student.setContactNum(request.contactNumber().trim());
        student.setTelegramHandle(TelegramHandle.normalize(request.telegramHandle()));
        student.replaceCoursesTaken(courseService.requireCourses(request.courses()));
        return profileMapper.toOwnProfile(student);
    }

    /**
     * Replaces the student's study preferences with the given list, at most one per course.
     *
     * @param studentId the student identifier
     * @param request the replacement preferences
     * @return the updated profile
     * @throws ValidationException if preferences contain a duplicate or untaken course
     */
    public StudentProfileResponse updatePreferences(UUID studentId, UpdatePreferencesRequest request) {
        Student student = requireStudent(studentId);
        List<StudyPreferenceDto> preferences = request.preferences();
        Set<String> courseCodes = new HashSet<>();
        for (StudyPreferenceDto preference : preferences) {
            if (!student.takesCourse(preference.course())) {
                throw new ValidationException("Choose one of the courses you are taking for each preference.");
            }
            if (!courseCodes.add(preference.course())) {
                throw new ValidationException("Each course can only have one study preference.");
            }
        }
        student.retainPreferencesFor(courseCodes);
        for (StudyPreferenceDto preference : preferences) {
            Course course = courseService.requireCourse(preference.course());
            student.saveStudyPreference(course, preference.meetingMode(), preference.groupFormat(),
                    preference.goals(), TimeSlotDto.toTimeSlots(preference.availability()));
        }
        return profileMapper.toOwnProfile(student);
    }

    /**
     * Loads a student by identifier.
     *
     * @param studentId the student identifier
     * @return the student
     * @throws ResourceNotFoundException if the student does not exist
     */
    @Transactional(readOnly = true)
    public Student requireStudent(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }
}
