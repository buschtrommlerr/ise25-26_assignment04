package de.seuhd.campuscoffee.data.persistence;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repository for persisting point-of-sale (POS) entities.
 */
public interface PosRepository extends JpaRepository<PosEntity, Long> {
    @Modifying
    @Transactional
    @Query(value = "ALTER SEQUENCE pos_seq RESTART WITH 1", nativeQuery = true)
    void resetSequence();

    Optional<PosEntity> findByOsmNodeId(Long osmNodeId);
}
