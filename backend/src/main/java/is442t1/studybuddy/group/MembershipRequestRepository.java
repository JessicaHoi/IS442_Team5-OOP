package is442t1.studybuddy.group;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipRequestRepository extends JpaRepository<MembershipRequest, UUID> {
    List<MembershipRequest> findByStudent_Id(UUID studentId);
}
