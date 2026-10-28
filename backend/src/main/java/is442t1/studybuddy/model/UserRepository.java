package is442t1.studybuddy.model;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Looks up any kind of account (student or administrator). */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);

    @Query(value = "select cast(id as varchar(36)) from users where id in (select id from systemadministrator) order by id for update",
            nativeQuery = true)
    List<String> lockAdministratorAccounts();

    @Query(value = "select cast(id as varchar(36)) from users where id = :userId for update", nativeQuery = true)
    Optional<String> lockAccount(@Param("userId") UUID userId);

    @Modifying
    @Query("""
            update User u set u.lastLogin = :lastLogin
            where u.id = :userId and u.password = :passwordHash
              and u.status = is442t1.studybuddy.model.enums.AccountStatus.ACTIVE
            """)
    int updateLastLoginForActiveAccount(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash,
            @Param("lastLogin") Instant lastLogin);
}
