package de.seuhd.campuscoffee.domain.ports;

import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.exceptions.PosNotFoundException;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for POS data operations.
 * This port is implemented by the data layer (adapter) and defines the contract
 * for persistence operations on Point of Sale entities.
 * Follows the hexagonal architecture pattern where the domain defines the port
 * and the infrastructure layer provides the adapter implementation.
 */
public interface PosDataService {
    /**
     * Clears all POS data from the data store.
     * This is typically used for testing or administrative purposes.
     * Warning: This operation is destructive and cannot be undone.
     */
    void clear();

    /**
     * Retrieves all POS entities from the data store.
     *
     * @return a list of all POS entities; never null, but may be empty
     */
    @NonNull List<Pos> getAll();

    /**
     * Retrieves a single POS entity by its unique identifier.
     *
     * @param id the unique identifier of the POS to retrieve; must not be null
     * @return the POS entity with the specified ID; never null
     * @throws PosNotFoundException if no POS exists with the given ID
     */
    @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException;

    /**
     * Creates a new POS or updates an existing one.
     * If the POS has an ID and exists in the data store, it will be updated.
     * If the POS has no ID (null), a new POS will be created.
     *
     * @param pos the POS entity to create or update; must not be null
     * @return the persisted POS entity with updated timestamps and ID; never null
     * @throws PosNotFoundException if attempting to update a POS that does not exist
     */
    @NonNull Pos upsert(@NonNull Pos pos) throws PosNotFoundException;

    /**
     * Finds a POS by its source OSM node ID.
     */
    @NonNull Optional<Pos> findByOsmNodeId(@NonNull Long osmNodeId);
}
