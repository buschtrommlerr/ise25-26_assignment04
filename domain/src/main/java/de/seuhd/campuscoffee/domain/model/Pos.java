package de.seuhd.campuscoffee.domain.model;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Domain record that stores the POS (Point of Sale) metadata.
 * This is an immutable value object - use the builder or toBuilder() to create modified copies.
 * Records provide automatic implementations of equals(), hashCode(), toString(), and accessors.
 *
 * @param id          the unique identifier; null when the POS has not been created yet
 * @param createdAt   timestamp set on POS creation
 * @param updatedAt   timestamp set on POS creation and update
 * @param name        the name of the POS
 * @param description a description of the POS
 * @param type        the type of POS (cafe, bakery, etc.)
 * @param campus      the campus location
 * @param street      street name
 * @param houseNumber house number (may include suffix such as "21a")
 * @param postalCode  postal code
 * @param city        city name
 * @param osmNodeId   optional: OSM node id this POS was imported from (for idempotence)
 */
@Builder(toBuilder = true)
public record Pos(
        @Nullable Long id,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt,
        @NonNull String name,
        @NonNull String description,
        @NonNull PosType type,
        @NonNull CampusType campus,
        @NonNull String street,
        @NonNull String houseNumber,
        @NonNull Integer postalCode,
        @NonNull String city,
        @Nullable Long osmNodeId
) implements Serializable { // serializable to allow cloning (see TestFixtures class).
    @Serial
    private static final long serialVersionUID = 1L;
}
