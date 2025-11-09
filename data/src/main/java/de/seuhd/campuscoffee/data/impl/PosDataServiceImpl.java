package de.seuhd.campuscoffee.data.impl;

import de.seuhd.campuscoffee.data.mapper.PosEntityMapper;
import de.seuhd.campuscoffee.data.persistence.PosEntity;
import de.seuhd.campuscoffee.data.persistence.PosRepository;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.exceptions.DuplicatePosNameException;
import de.seuhd.campuscoffee.domain.exceptions.PosNotFoundException;
import de.seuhd.campuscoffee.domain.ports.PosDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the POS data service that the domain layer provides as a port.
 * This layer is responsible for data access and persistence.
 * Business logic should be in the service layer.
 */
@Service
@RequiredArgsConstructor
class PosDataServiceImpl implements PosDataService {
    private final PosRepository posRepository;
    private final PosEntityMapper posEntityMapper;

    @Override
    public void clear() {
        posRepository.deleteAllInBatch();
        posRepository.flush();
        posRepository.resetSequence();
    }

    @Override
    public @NonNull List<Pos> getAll() {
        return posRepository.findAll().stream()
                .map(posEntityMapper::fromEntity)
                .toList();
    }

    @Override
    public @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException {
        return posRepository.findById(id)
                .map(posEntityMapper::fromEntity)
                .orElseThrow(() -> new PosNotFoundException(id));
    }

    @Override
    public @NonNull Pos upsert(@NonNull Pos pos) {
        // Map POS domain object to entity and save
        try {
            if (pos.id() == null) {
                // Create new POS
                return posEntityMapper.fromEntity(
                        posRepository.saveAndFlush(posEntityMapper.toEntity(pos))
                );
            }

            // Update existing POS
            PosEntity posEntity = posRepository.findById(pos.id())
                    .orElseThrow(() -> new PosNotFoundException(pos.id()));

            // Use mapper to update entity fields automatically
            // Note: timestamps are managed by JPA lifecycle callbacks (@PreUpdate)
            posEntityMapper.updateEntity(pos, posEntity);

            return posEntityMapper.fromEntity(posRepository.saveAndFlush(posEntity));
        } catch (DataIntegrityViolationException e) {
            // Translate database constraint violations to domain exceptions
            // This is the adapter's responsibility in hexagonal architecture
            if (isDuplicateNameConstraintViolation(e)) {
                throw new DuplicatePosNameException(pos.name());
            }
            // Re-throw if it's a different constraint violation
            throw e;
        }
    }

    @Override
    public @NonNull Optional<Pos> findByOsmNodeId(@NonNull Long osmNodeId) {
        return posRepository.findByOsmNodeId(osmNodeId).map(posEntityMapper::fromEntity);
    }

    /**
     * Checks if the exception is due to duplicate POS name constraint violation.
     */
    private static boolean isDuplicateNameConstraintViolation(DataIntegrityViolationException e) {
        // Database constraint name for unique pos name
        final String POS_NAME_CONSTRAINT = "pos_name_key";

        // Check the exception message and root cause for the constraint name
        String message = e.getMessage();
        if (message != null && message.contains(POS_NAME_CONSTRAINT)) {
            return true;
        }

        // Also check root cause for constraint violations
        Throwable cause = e.getRootCause();
        if (cause != null) {
            String causeMessage = cause.getMessage();
            return causeMessage != null && causeMessage.contains(POS_NAME_CONSTRAINT);
        }

        return false;
    }
}
