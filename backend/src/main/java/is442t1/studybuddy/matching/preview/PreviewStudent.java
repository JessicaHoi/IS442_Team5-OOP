package is442t1.studybuddy.matching.preview;

import java.util.List;
import java.util.UUID;

/**
 * A student the administrator can pick as the preview sample.
 *
 * @param id      the student
 * @param name    their name
 * @param courses courses they have study preferences for, first one is the default
 */
public record PreviewStudent(UUID id, String name, List<String> courses) {
}
