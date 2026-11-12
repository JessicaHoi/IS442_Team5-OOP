package is442t1.studybuddy.group;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence operations for requests to join study groups. */
@Repository
public interface MembershipRequestRepository extends JpaRepository<MembershipRequest, UUID> {

    /**
     * Finds all join requests submitted by a student.
     *
     * @param studentId the student's identifier
     * @return requests made by that student
     */
    List<MembershipRequest> findByStudent_Id(UUID studentId);
}
