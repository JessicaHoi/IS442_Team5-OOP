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

    public StudentService(StudentRepository studentRepository, CourseService courseService,
            BuddyStateService buddyStateService, StudentProfileMapper profileMapper) {
        this.studentRepository = studentRepository;
        this.courseService = courseService;
        this.buddyStateService = buddyStateService;
        this.profileMapper = profileMapper;
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getOwnProfile(UUID studentId) {
        return profileMapper.toOwnProfile(requireStudent(studentId));
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getPublicProfile(UUID viewerId, UUID studentId) {
        if (viewerId.equals(studentId)) {
            return getOwnProfile(studentId);
        }
        Student student = requireStudent(studentId);
        return profileMapper.toPublicProfile(student, buddyStateService.stateBetween(viewerId, studentId));
    }

    public StudentProfileResponse updateProfile(UUID studentId, UpdateProfileRequest request) {
        Student student = requireStudent(studentId);
        student.setName(request.name().trim());
        student.setSchool(request.school().trim());
        student.setProgram(request.programme().trim());
        student.setYearOfStudy(request.yearOfStudy());
        student.setContactNum(request.contactNumber().trim());
        student.replaceCoursesTaken(courseService.requireCourses(request.courses()));
        return profileMapper.toOwnProfile(student);
    }

    /** Replaces the student's study preferences with the given list (one per course). */
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

    /** Loads a student, or fails with a 404. Shared with other services. */
    @Transactional(readOnly = true)
    public Student requireStudent(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }
}
