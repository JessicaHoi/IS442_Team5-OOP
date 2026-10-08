package is442t1.studybuddy.group;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import is442t1.studybuddy.model.enums.GroupStatus;

@Repository
public interface StudyGroupRepository extends JpaRepository<StudyGroup, UUID> {

    List<StudyGroup> findByStatusOrderByCreatedAtDesc(GroupStatus status);

    List<StudyGroup> findByStatusAndCourse_CourseCodeOrderByCreatedAtDesc(GroupStatus status, String courseCode);

    @Query("select g from StudyGroup g join g.members m where m.id = :studentId")
    List<StudyGroup> findByMemberId(@Param("studentId") UUID studentId);
}
