package is442t1.studybuddy.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import is442t1.studybuddy.model.enums.Role;

/**
 * Restricts a controller (or one of its methods) to a role. A method-level
 * annotation overrides the class-level one. Without it, any signed-in user may call.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    Role value();
}
