package de.seuhd.campuscoffee.data.persistence;

import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.PosType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Database entity for a point-of-sale (POS).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pos")
public class PosEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pos_sequence_generator")
    @SequenceGenerator(name = "pos_sequence_generator", sequenceName = "pos_seq", allocationSize = 1)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(unique = true)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private PosType type;

    @Enumerated(EnumType.STRING)
    private CampusType campus;

    @Embedded
    private AddressEntity address;

    @Column(name = "osm_node_id", unique = true)
    private Long osmNodeId;

    /**
     * JPA lifecycle callback: set timestamps before persisting a new entity.
     * This ensures timestamps reflect actual database operation time.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        createdAt = now;
        updatedAt = now;
    }

    /**
     * JPA lifecycle callback: update timestamp before updating entity.
     * This ensures timestamps reflect actual database operation time.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneId.of("UTC"));
    }
}
