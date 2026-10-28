package is442t1.studybuddy.group;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyGroupLeaderRepository extends JpaRepository<StudyGroupLeader, UUID> {

    /**
     * Turns an existing student into a {@link StudyGroupLeader}. JPA cannot
     * change an entity's class, but with JOINED inheritance the subclass is
     * just an extra row keyed by the same id, so inserting that row is enough.
     * The persistence context is cleared afterwards so the student is
     * reloaded as a leader.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "insert into study_group_leader (id) values (:studentId)", nativeQuery = true)
    void promoteStudent(@Param("studentId") UUID studentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            delete from study_group_leader where id = :studentId
              and not exists (select 1 from study_group where leader_id = :studentId)
            """, nativeQuery = true)
    void demoteIfNoGroups(@Param("studentId") UUID studentId);
}
