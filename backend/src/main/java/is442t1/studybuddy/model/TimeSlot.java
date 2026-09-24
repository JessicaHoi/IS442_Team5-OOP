package is442t1.studybuddy.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public record TimeSlot(
        // "day" is a reserved word in H2 db, so i used a alternative colname
        @Enumerated(EnumType.STRING)
        @Column(name = "day_of_week", nullable = false)
        DayOfWeek day,

        @Column(nullable = false)
        LocalTime startTime,

        @Column(nullable = false)
        LocalTime endTime
) {
}
