package is442t1.studybuddy.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import is442t1.studybuddy.common.exception.ValidationException;
import is442t1.studybuddy.course.Course;
import is442t1.studybuddy.model.TimeSlot;
import is442t1.studybuddy.model.enums.GroupPreference;
import is442t1.studybuddy.model.enums.StudyGoal;
import is442t1.studybuddy.model.enums.StudyMode;

/** Profile and preference rules on the {@link Student} entity. */
class StudentTest {

    private static final Course IS442 = course("IS442");
    private static final Course IS212 = course("IS212");
    private static final TimeSlot WEDNESDAY_EVENING =
            new TimeSlot(DayOfWeek.WEDNESDAY, LocalTime.of(19, 0), LocalTime.of(21, 0));

    @Test
    void keepsOnePreferencePerCourse() {
        Student student = studentTaking(IS442, IS212);

        student.saveStudyPreference(IS442, StudyMode.ONLINE, GroupPreference.EITHER,
                Set.of(StudyGoal.CONCEPT_REVIEW), List.of(WEDNESDAY_EVENING));
        student.saveStudyPreference(IS212, StudyMode.ONLINE, GroupPreference.EITHER,
                Set.of(StudyGoal.CONCEPT_REVIEW), List.of(WEDNESDAY_EVENING));
        student.saveStudyPreference(IS442, StudyMode.IN_PERSON, GroupPreference.SMALL_GROUP,
                Set.of(StudyGoal.EXAM_PREPARATION), List.of(WEDNESDAY_EVENING));

        assertThat(student.getStudyPreferences()).hasSize(2);
        StudyPreference updated = student.findPreferenceFor("IS442").orElseThrow();
        assertThat(updated.getStudyMode()).isEqualTo(StudyMode.IN_PERSON);
        assertThat(updated.getGroupPreference()).isEqualTo(GroupPreference.SMALL_GROUP);
        assertThat(updated.getStudyGoals()).containsExactly(StudyGoal.EXAM_PREPARATION);
    }

    @Test
    void droppingACourseRemovesItsPreference() {
        Student student = studentTaking(IS442, IS212);
        student.saveStudyPreference(IS212, StudyMode.ONLINE, GroupPreference.EITHER,
                Set.of(StudyGoal.CONCEPT_REVIEW), List.of(WEDNESDAY_EVENING));

        student.replaceCoursesTaken(List.of(IS442));

        assertThat(student.getStudyPreferences()).isEmpty();
        assertThat(student.takesCourse("IS212")).isFalse();
    }

    @Test
    void rejectsOverlappingAvailability() {
        Student student = studentTaking(IS442);
        TimeSlot overlapping = new TimeSlot(DayOfWeek.WEDNESDAY, LocalTime.of(20, 0), LocalTime.of(22, 0));

        assertThatThrownBy(() -> student.saveStudyPreference(IS442, StudyMode.ONLINE, GroupPreference.EITHER,
                Set.of(StudyGoal.CONCEPT_REVIEW), List.of(WEDNESDAY_EVENING, overlapping)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("overlap");
    }

    private static Student studentTaking(Course... courses) {
        Student student = new Student();
        student.replaceCoursesTaken(List.of(courses));
        return student;
    }

    private static Course course(String code) {
        Course course = new Course();
        course.setCourseCode(code);
        return course;
    }
}
