package is442t1.studybuddy.admin;

import is442t1.studybuddy.model.User;
import is442t1.studybuddy.model.enums.Role;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "systemadministrator")
public class SystemAdministrator extends User {

    private static final String DISPLAY_NAME = "System Administrator";

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }
}
