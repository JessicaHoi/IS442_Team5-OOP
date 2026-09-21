package is442t1.studybuddy.admin;

import is442t1.studybuddy.model.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "student")
public class SystemAdministrator extends User {
}