package is442t1.studybuddy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import is442t1.studybuddy.model.enums.Role;

@Entity 
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User extends BaseEntity {
    
    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash, never the plain-text password. */
    @Column(nullable = false)
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /** Role used for access control. Each concrete account type decides its own. */
    public abstract Role getRole();

    /** Name shown in the navigation bar after signing in. */
    public abstract String getDisplayName();
}
