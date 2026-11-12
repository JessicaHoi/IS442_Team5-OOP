package is442t1.studybuddy.model;

import java.time.Instant;
import java.util.Locale;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import is442t1.studybuddy.model.enums.AccountStatus;
import is442t1.studybuddy.model.enums.Role;

/** Base entity for a student or administrator account. */
@Entity 
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User extends BaseEntity {

    /** Creates an account with active status. */
    public User() {
    }
    
    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash, never the plain-text password. */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'ACTIVE'")
    private AccountStatus status = AccountStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private Instant createdAt;

    private Instant lastLogin;

    /**
     * Returns the account status.
     *
     * @return the account status
     */
    public AccountStatus getStatus() {
        return status;
    }

    /**
     * Sets the new account status.
     *
     * @param status the new account status
     */
    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    /**
     * Reports whether the account status is active.
     *
     * @return whether the account status is active
     */
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /**
     * Returns the account creation time.
     *
     * @return the account creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the last successful login time, or {@code null} if not recorded.
     *
     * @return the last successful login time, or {@code null} if not recorded
     */
    public Instant getLastLogin() {
        return lastLogin;
    }

    /**
     * Sets the last successful login time.
     *
     * @param lastLogin the last successful login time
     */
    public void setLastLogin(Instant lastLogin) {
        this.lastLogin = lastLogin;
    }

    /**
     * Returns the account email.
     *
     * @return the account email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Stores the email trimmed and in lower case, matching login's case-insensitive lookup.
     *
     * @param email the email to store, or {@code null}
     */
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the stored password hash.
     *
     * @return the stored password hash
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password hash to store.
     *
     * @param password the password hash to store
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the role used for access control.
     *
     * @return the role used for access control
     */
    public abstract Role getRole();

    /**
     * Returns the name shown in the navigation bar after signing in.
     *
     * @return the name shown in the navigation bar after signing in
     */
    public abstract String getDisplayName();
}
