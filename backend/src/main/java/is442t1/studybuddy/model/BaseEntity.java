package is442t1.studybuddy.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

/** Base class for persisted entities identified by a generated UUID. */
@MappedSuperclass
public abstract class BaseEntity {

    /** Creates an entity without an assigned identifier. */
    public BaseEntity() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Returns the entity identifier.
     *
     * @return the identifier, or {@code null} before one is assigned
     */
    public UUID getId() {
        return id;
    }

    /**
     * Assigns the entity identifier.
     *
     * @param id the identifier
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Checks whether an identifier has been assigned.
     *
     * @return whether the entity has no identifier
     */
    public boolean isNew() {
        return this.id == null;
    }
}