package is442t1.studybuddy.matching;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.exception.ResourceNotFoundException;
import is442t1.studybuddy.common.exception.ValidationException;
import is442t1.studybuddy.connection.BuddyState;
import is442t1.studybuddy.connection.BuddyStateService;
import is442t1.studybuddy.course.Course;
import is442t1.studybuddy.matching.strategy.RankingStrategyResolver;
import is442t1.studybuddy.model.TimeSlot;
import is442t1.studybuddy.model.enums.GroupPreference;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;
import is442t1.studybuddy.student.Student;
import is442t1.studybuddy.student.StudentProfileMapper;
import is442t1.studybuddy.student.StudentProfileResponse;
import is442t1.studybuddy.student.StudentRepository;
import is442t1.studybuddy.student.StudyPreference;

/**
 * Searches students, scores them with the administrator's criterion weights and
 * orders them with the configured ranking strategy (same rules as the mock API).
 */
@Service
public class MatchingService {

    private static final int FULL_AVAILABILITY_MINUTES = 180;

    private final StudentRepository studentRepository;
    private final StudentProfileMapper profileMapper;
    private final BuddyStateService buddyStateService;
    private final MatchingConfigService configService;
    private final RankingStrategyResolver strategyResolver;

    public MatchingService(StudentRepository studentRepository, StudentProfileMapper profileMapper,
            BuddyStateService buddyStateService, MatchingConfigService configService,
            RankingStrategyResolver strategyResolver) {
        this.studentRepository = studentRepository;
        this.profileMapper = profileMapper;
        this.buddyStateService = buddyStateService;
        this.configService = configService;
        this.strategyResolver = strategyResolver;
    }

    @Transactional(readOnly = true)
    public List<MatchResult> search(UUID viewerId, String course, StudyGoal goal, String dayCode,
            StudyMode studyMode, GroupPreference groupFormat) {
        DayOfWeek day = parseDay(dayCode);
        Student viewer = studentRepository.findById(viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        StudyPreference viewerPreference = preferenceFor(viewer, course);
        String targetCourse = course != null && !course.isBlank()
                ? course
                : viewerPreference == null ? null : viewerPreference.getCourse().getCourseCode();
        MatchingRules rules = configService.currentRules();

        return studentRepository.findAll().stream()
                .filter(Student::isActive)
                .filter(candidate -> !candidate.hasId(viewerId))
                .filter(candidate -> matchesFilters(candidate, course, goal, day, studyMode, groupFormat))
                .map(candidate -> toResult(viewer, viewerPreference, candidate, targetCourse, rules))
                .sorted(strategyResolver.resolve(rules.strategy()).comparator())
                .toList();
    }

    private MatchResult toResult(Student viewer, StudyPreference viewerPreference, Student candidate,
            String targetCourse, MatchingRules rules) {
        StudyPreference candidatePreference = preferenceFor(candidate, targetCourse);
        int overlapMinutes = overlapMinutes(viewerPreference, candidatePreference);
        MatchScoreBreakdown breakdown = breakdown(viewer, candidate, viewerPreference, candidatePreference,
                targetCourse, overlapMinutes);
        int score = rules.score(breakdown);

        BuddyState buddyState = buddyStateService.stateBetween(viewer.getId(), candidate.getId());
        StudentProfileResponse profile = profileMapper.toPublicProfile(candidate, buddyState);
        List<String> sharedCourses = candidate.getCoursesTaken().stream()
                .map(Course::getCourseCode)
                .filter(viewer::takesCourse)
                .sorted()
                .toList();

        return new MatchResult(profile, score, breakdown, overlapMinutes, sharedCourses, buddyState.status());
    }

    private static MatchScoreBreakdown breakdown(Student viewer, Student candidate,
            StudyPreference viewerPreference, StudyPreference candidatePreference,
            String targetCourse, int overlapMinutes) {
        return new MatchScoreBreakdown(
                courseScore(viewer, candidate, candidatePreference, targetCourse),
                Math.min(1.0, (double) overlapMinutes / FULL_AVAILABILITY_MINUTES),
                compatibility(meetingMode(viewerPreference), meetingMode(candidatePreference)),
                jaccard(studyGoals(viewerPreference), studyGoals(candidatePreference)),
                compatibility(groupFormat(viewerPreference), groupFormat(candidatePreference)));
    }

    private static double courseScore(Student viewer, Student candidate, StudyPreference candidatePreference,
            String targetCourse) {
        if (targetCourse == null || targetCourse.isBlank()) {
            return candidate.getCoursesTaken().stream().anyMatch(course -> viewer.takesCourse(course.getCourseCode()))
                    ? 0.5 : 0;
        }
        if (!candidate.takesCourse(targetCourse)) {
            return candidate.getCoursesTaken().stream().anyMatch(course -> viewer.takesCourse(course.getCourseCode()))
                    ? 0.25 : 0;
        }
        return candidatePreference != null
                && candidatePreference.getCourse().getCourseCode().equals(targetCourse) ? 1 : 0.75;
    }

    private static boolean matchesFilters(Student candidate, String course, StudyGoal goal, DayOfWeek day,
            StudyMode studyMode, GroupPreference groupFormat) {
        if (course != null && !course.isBlank() && !candidate.takesCourse(course)) {
            return false;
        }
        StudyPreference preference = preferenceFor(candidate, course);
        if (goal != null && (preference == null || !preference.getStudyGoals().contains(goal))) {
            return false;
        }
        if (day != null && (preference == null || preference.getAvailability().stream()
                .noneMatch(slot -> slot.day() == day))) {
            return false;
        }
        return compatibleFilter(meetingMode(preference), studyMode)
                && compatibleFilter(groupFormat(preference), groupFormat);
    }

    private static boolean compatibleFilter(StudyMode candidateValue, StudyMode wanted) {
        return wanted == null || wanted == StudyMode.EITHER
                || candidateValue == wanted || candidateValue == StudyMode.EITHER;
    }

    private static boolean compatibleFilter(GroupPreference candidateValue, GroupPreference wanted) {
        return wanted == null || wanted == GroupPreference.EITHER
                || candidateValue == wanted || candidateValue == GroupPreference.EITHER;
    }

    private static StudyPreference preferenceFor(Student student, String course) {
        if (course != null && !course.isBlank()) {
            StudyPreference exact = student.findPreferenceFor(course).orElse(null);
            if (exact != null) {
                return exact;
            }
        }
        return student.getStudyPreferences().stream().findFirst().orElse(null);
    }

    private static StudyMode meetingMode(StudyPreference preference) {
        return preference == null || preference.getStudyMode() == null ? StudyMode.EITHER : preference.getStudyMode();
    }

    private static GroupPreference groupFormat(StudyPreference preference) {
        return preference == null || preference.getGroupPreference() == null
                ? GroupPreference.EITHER : preference.getGroupPreference();
    }

    private static Set<StudyGoal> studyGoals(StudyPreference preference) {
        return preference == null ? Set.of() : preference.getStudyGoals();
    }

    private static double compatibility(Enum<?> first, Enum<?> second) {
        if (first == second) {
            return 1;
        }
        if ("EITHER".equals(first.name()) || "EITHER".equals(second.name())) {
            return 0.75;
        }
        return 0;
    }

    private static double jaccard(Set<StudyGoal> first, Set<StudyGoal> second) {
        if (first.isEmpty() && second.isEmpty()) {
            return 0;
        }
        long shared = first.stream().filter(second::contains).count();
        long union = first.size() + second.size() - shared;
        return (double) shared / union;
    }

    private static int overlapMinutes(StudyPreference first, StudyPreference second) {
        if (first == null || second == null) {
            return 0;
        }
        return (int) first.getAvailability().stream()
                .mapToLong(left -> second.getAvailability().stream()
                        .filter(right -> left.day() == right.day())
                        .mapToLong(right -> overlapMinutes(left, right))
                        .sum())
                .sum();
    }

    private static long overlapMinutes(TimeSlot first, TimeSlot second) {
        if (!first.overlaps(second)) {
            return 0;
        }
        var start = first.startTime().isAfter(second.startTime()) ? first.startTime() : second.startTime();
        var end = first.endTime().isBefore(second.endTime()) ? first.endTime() : second.endTime();
        return Duration.between(start, end).toMinutes();
    }

    private static DayOfWeek parseDay(String dayCode) {
        if (dayCode == null || dayCode.isBlank()) {
            return null;
        }
        if (dayCode.length() != 3) {
            throw new ValidationException("Choose a valid day for the availability filter.");
        }
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day.name().startsWith(dayCode.toUpperCase())) {
                return day;
            }
        }
        throw new ValidationException("Choose a valid day for the availability filter.");
    }
}
