package de.seuhd.campuscoffee.domain.impl;

import de.seuhd.campuscoffee.domain.exceptions.DuplicatePosNameException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeMissingFieldsException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.exceptions.PosNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import de.seuhd.campuscoffee.domain.ports.PosDataService;
import de.seuhd.campuscoffee.domain.ports.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of the POS service that handles business logic related to POS entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosServiceImpl implements PosService {
    private final PosDataService posDataService;
    private final OsmDataService osmDataService;

    @Override
    public void clear() {
        log.warn("Clearing all POS data");
        posDataService.clear();
    }

    @Override
    public @NonNull List<Pos> getAll() {
        log.debug("Retrieving all POS");
        return posDataService.getAll();
    }

    @Override
    public @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException {
        log.debug("Retrieving POS with ID: {}", id);
        return posDataService.getById(id);
    }

    @Override
    public @NonNull Pos upsert(@NonNull Pos pos) throws PosNotFoundException {
        if (pos.id() == null) {
            // Create new POS
            log.info("Creating new POS: {}", pos.name());
            return performUpsert(pos);
        } else {
            // Update existing POS
            log.info("Updating POS with ID: {}", pos.id());
            // POS ID must be set
            Objects.requireNonNull(pos.id());
            // POS must exist in the database before the update
            posDataService.getById(pos.id());
            return performUpsert(pos);
        }
    }

    @Override
    public @NonNull Pos importFromOsmNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Importing POS from OpenStreetMap node {}...", nodeId);

        // Fetch the OSM node data using the port
        OsmNode osmNode = osmDataService.fetchNode(nodeId);

        // Convert OSM node to POS domain object and upsert it
        // TODO: Implement the actual conversion (the response is currently hard-coded).
        Pos savedPos = upsert(convertOsmNodeToPos(osmNode));
        log.info("Successfully imported POS '{}' from OSM node {}", savedPos.name(), nodeId);

        return savedPos;
    }

    /**
     * Converts an OSM node to a POS domain object.
     * This implementation maps OSM tags to POS fields with validations and type mapping.
     */
    private @NonNull Pos convertOsmNodeToPos(@NonNull OsmNode osmNode) {
        Map<String, String> tags = osmNode.tags();

        // Name (prefer name:de, fallback name)
        String name = firstNonBlank(tags.get("name:de"), tags.get("name"));
        if (isBlank(name)) {
            throw new OsmNodeMissingFieldsException(osmNode.nodeId());
        }

        // Beschreibung optional
        String description = firstNonBlank(tags.get("description"), tags.get("note"), "Imported from OSM node " + osmNode.nodeId());

        // Adresse
        String street = tags.get("addr:street");
        String houseNumberRaw = tags.get("addr:housenumber");
        String postalCodeRaw = tags.get("addr:postcode");
        String city = tags.get("addr:city");

        if (isBlank(street) || isBlank(houseNumberRaw) || isBlank(postalCodeRaw) || isBlank(city)) {
            throw new OsmNodeMissingFieldsException(osmNode.nodeId());
        }

        Integer postalCode;
        try {
            postalCode = Integer.parseInt(postalCodeRaw.trim());
        } catch (NumberFormatException e) {
            throw new OsmNodeMissingFieldsException(osmNode.nodeId());
        }

        // Typ ermitteln aus amenity oder shop
        PosType type = mapType(tags.get("amenity"), tags.get("shop"));

        // Campus heuristisch: hier nicht ableitbar -> Default ALTSTADT (Minimalinvasiv, könnte zukünftig verbessert werden)
        CampusType campus = CampusType.ALTSTADT;

        return Pos.builder()
                .name(name.trim())
                .description(description.trim())
                .type(type)
                .campus(campus)
                .street(street.trim())
                .houseNumber(houseNumberRaw.trim())
                .postalCode(postalCode)
                .city(city.trim())
                .build();
    }

    private PosType mapType(String amenity, String shop) {
        String source = firstNonBlank(amenity, shop, "");
        if (isBlank(source)) return PosType.OTHER;
        source = source.toLowerCase(Locale.ROOT);
        return switch (source) {
            case "cafe" -> PosType.CAFE;
            case "vending_machine" -> PosType.VENDING_MACHINE;
            case "bakery" -> PosType.BAKERY;
            case "cafeteria", "canteen" -> PosType.CAFETERIA;
            default -> PosType.OTHER;
        };
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (!isBlank(v)) return v;
        }
        return null;
    }

    /**
     * Performs the actual upsert operation with consistent error handling and logging.
     * Database constraint enforces name uniqueness - data layer will throw DuplicatePosNameException if violated.
     * JPA lifecycle callbacks (@PrePersist/@PreUpdate) set timestamps automatically.
     *
     * @param pos the POS to upsert
     * @return the persisted POS with updated ID and timestamps
     * @throws DuplicatePosNameException if a POS with the same name already exists
     */
    private @NonNull Pos performUpsert(@NonNull Pos pos) throws DuplicatePosNameException {
        try {
            Pos upsertedPos = posDataService.upsert(pos);
            log.info("Successfully upserted POS with ID: {}", upsertedPos.id());
            return upsertedPos;
        } catch (DuplicatePosNameException e) {
            log.error("Error upserting POS '{}': {}", pos.name(), e.getMessage());
            throw e;
        }
    }
}
