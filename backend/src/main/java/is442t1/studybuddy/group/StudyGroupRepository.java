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

@Repository
public interface StudyGroupRepository extends JpaRepository<StudyGroup, UUID> {

    @Query("""
            select m.id as userId, count(g) as usageCount
            from StudyGroup g join g.members m
            group by m.id
            """)
    List<AccountUsageCount> countMembershipsByStudent();

    @Query("select g.leader.id as userId, count(g) as usageCount from StudyGroup g group by g.leader.id")
    List<AccountUsageCount> countLeadershipsByStudent();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from StudyGroup g where g.id = :groupId")
    Optional<StudyGroup> findForUpdate(@Param("groupId") UUID groupId);

    List<StudyGroup> findByStatusOrderByCreatedAtDesc(GroupStatus status);

    List<StudyGroup> findByStatusAndCourse_CourseCodeOrderByCreatedAtDesc(GroupStatus status, String courseCode);

    @Query("select g from StudyGroup g join g.members m where m.id = :studentId")
    List<StudyGroup> findByMemberId(@Param("studentId") UUID studentId);

    List<StudyGroup> findByLeader_Id(UUID leaderId);

    boolean existsByLeader_Id(UUID leaderId);

    long countByLeader_Id(UUID leaderId);
}
