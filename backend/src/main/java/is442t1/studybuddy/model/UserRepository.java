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
    /**
     * Looks up an account without regard to email case.
     *
     * @param email the email to find
     * @return the matching account, if present
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Locks administrator account rows in identifier order.
     *
     * @return the locked account identifiers as strings
     */
    @Query(value = "select cast(id as varchar(36)) from users where id in (select id from systemadministrator) order by id for update",
            nativeQuery = true)
    List<String> lockAdministratorAccounts();

    /**
     * Locks an account row.
     *
     * @param userId the account identifier
     * @return the locked identifier as a string, if the account exists
     */
    @Query(value = "select cast(id as varchar(36)) from users where id = :userId for update", nativeQuery = true)
    Optional<String> lockAccount(@Param("userId") UUID userId);

    /**
     * Records a login only if the account is active and its password hash still matches.
     *
     * @param userId the account identifier
     * @param passwordHash the password hash to check
     * @param lastLogin the login time to record
     * @return the number of updated rows
     */
    @Modifying
    @Query("""
            update User u set u.lastLogin = :lastLogin
            where u.id = :userId and u.password = :passwordHash
              and u.status = is442t1.studybuddy.model.enums.AccountStatus.ACTIVE
            """)
    int updateLastLoginForActiveAccount(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash,
            @Param("lastLogin") Instant lastLogin);
}
