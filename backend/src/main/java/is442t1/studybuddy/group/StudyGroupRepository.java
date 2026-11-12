package is442t1.studybuddy.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import is442t1.studybuddy.model.enums.GroupStatus;
import is442t1.studybuddy.model.AccountUsageCount;
import jakarta.persistence.LockModeType;

/** Persistence operations for study groups and account-usage totals. */
@Repository
public interface StudyGroupRepository extends JpaRepository<StudyGroup, UUID> {

    /**
     * Counts a student's memberships across all study groups.
     *
     * @return membership counts grouped by student identifier
     */
    @Query("""
            select m.id as userId, count(g) as usageCount
            from StudyGroup g join g.members m
            group by m.id
            """)
    List<AccountUsageCount> countMembershipsByStudent();

    /**
     * Counts the groups led by each student.
     *
     * @return leadership counts grouped by student identifier
     */
    @Query("select g.leader.id as userId, count(g) as usageCount from StudyGroup g group by g.leader.id")
    List<AccountUsageCount> countLeadershipsByStudent();

    /**
     * Finds and locks a group for the duration of the transaction.
     *
     * @param groupId the group's identifier
     * @return the locked group, or empty if it does not exist
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from StudyGroup g where g.id = :groupId")
    Optional<StudyGroup> findForUpdate(@Param("groupId") UUID groupId);

    /**
     * Finds groups with the supplied status, newest first.
     *
     * @param status the status to match
     * @return matching groups in descending creation order
     */
    List<StudyGroup> findByStatusOrderByCreatedAtDesc(GroupStatus status);

    /**
     * Finds groups with the supplied status and course, newest first.
     *
     * @param status the status to match
     * @param courseCode the course code to match
     * @return matching groups in descending creation order
     */
    List<StudyGroup> findByStatusAndCourse_CourseCodeOrderByCreatedAtDesc(GroupStatus status, String courseCode);

    /**
     * Finds all groups containing the specified student.
     *
     * @param studentId the student's identifier
     * @return the groups containing that student
     */
    @Query("select g from StudyGroup g join g.members m where m.id = :studentId")
    List<StudyGroup> findByMemberId(@Param("studentId") UUID studentId);

    /**
     * Finds groups led by a student.
     *
     * @param leaderId the leader's identifier
     * @return groups led by that student
     */
    List<StudyGroup> findByLeader_Id(UUID leaderId);

    /**
     * Checks whether a student leads any groups.
     *
     * @param leaderId the leader's identifier
     * @return {@code true} if at least one group is led by that student
     */
    boolean existsByLeader_Id(UUID leaderId);

    /**
     * Counts groups led by a student.
     *
     * @param leaderId the leader's identifier
     * @return the number of groups led by that student
     */
    long countByLeader_Id(UUID leaderId);
}
