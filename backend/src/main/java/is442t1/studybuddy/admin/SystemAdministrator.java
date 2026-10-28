package is442t1.studybuddy.admin;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.enums.Role;

@Entity
@Table(name = "systemadministrator")
public class SystemAdministrator extends User {

    @Column(nullable = false)
    @ColumnDefault("'System Administrator'")
    private String name = "System Administrator";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public String getDisplayName() {
        return name;
    }
}
