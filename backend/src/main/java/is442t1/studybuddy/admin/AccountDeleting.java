package is442t1.studybuddy.admin;

import java.util.UUID;

/** A student's account is about to be deleted. Published before anything is removed, so listeners can still read their data. */
public record AccountDeleting(UUID studentId) {
}
