package de.seuhd.campuscoffee.data.mapper;

import de.seuhd.campuscoffee.data.persistence.AddressEntity;
import de.seuhd.campuscoffee.data.persistence.PosEntity;
import de.seuhd.campuscoffee.domain.model.Pos;
import org.mapstruct.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * MapStruct mapper for converting between domain models and JPA entities.
 * This mapper handles the translation between the {@link Pos} domain model and the
 * {@link PosEntity} persistence entity, including the embedded {@link AddressEntity}.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Maps flat address fields from domain to embedded AddressEntity in JPA entity</li>
 *   <li>Handles house number parsing: splits "21a" into numeric (21) and suffix ('a') parts</li>
 *   <li>Provides update functionality that preserves JPA-managed fields (id, timestamps)</li>
 * </ul>
 * <p>
 * This is part of the data layer adapter in the hexagonal architecture, enabling the
 * domain layer to remain independent of persistence concerns.
 */
@Mapper(componentModel = "spring")
@ConditionalOnMissingBean // prevent IntelliJ warning about duplicate beans
public interface PosEntityMapper {
    // TODO: The address logic is deliberately misaligned between the domain and persistence layers to demonstrate the
    //  abstractions that hexagonal architecture provides.

    /**
     * Converts a JPA entity to a domain model.
     * Maps the embedded AddressEntity fields to the flat structure in the domain model.
     * House numbers are merged (e.g., numeric=21 + suffix='a' becomes "21a").
     *
     * @param source the JPA entity to convert; may be null
     * @return the domain model, or null if source is null
     */
    @Mapping(source = "address.street", target = "street")
    @Mapping(source = "address.postalCode", target = "postalCode")
    @Mapping(source = "address.city", target = "city")
    @Mapping(target = "houseNumber", expression = "java(mergeHouseNumber(source))")
    // map osmNodeId
    @Mapping(source = "osmNodeId", target = "osmNodeId")
    Pos fromEntity(PosEntity source);

    /**
     * Converts a domain model to a JPA entity.
     * Creates a new AddressEntity and parses the house number string into numeric and suffix parts.
     *
     * @param source the domain model to convert; may be null
     * @return the JPA entity, or null if source is null
     */
    @Mapping(target = "address", expression = "java(splitHouseNumber(source, new AddressEntity()))")
    // map osmNodeId
    @Mapping(source = "osmNodeId", target = "osmNodeId")
    PosEntity toEntity(Pos source);

    /**
     * Updates an existing JPA entity with data from the domain model.
     * This method is intended for update operations where the entity already exists.
     * JPA-managed fields (id, createdAt, updatedAt) are preserved and not overwritten.
     * The address is updated in place rather than being replaced, preserving the entity relationship.
     *
     * @param source the domain model containing the new data; must not be null
     * @param target the existing JPA entity to update; must not be null
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "address", expression = "java(splitHouseNumber(source, target.getAddress()))")
    // map osmNodeId as updatable
    @Mapping(source = "osmNodeId", target = "osmNodeId")
    void updateEntity(Pos source, @MappingTarget PosEntity target);

    /**
     * Merges the numeric house number and suffix from an entity into a single string.
     * This is the inverse operation of {@link #splitHouseNumber(Pos, AddressEntity)}.
     * <p>
     * Examples:
     * <ul>
     *   <li>houseNumber=21, suffix='a' → "21a"</li>
     *   <li>houseNumber=10, suffix=null → "10"</li>
     *   <li>houseNumber=null → null</li>
     * </ul>
     *
     * @param source the PosEntity containing the address; may be null
     * @return the merged house number string, or null if the entity has no address or house number
     */
    @SuppressWarnings("unused")
    default String mergeHouseNumber(PosEntity source) {
        if (source.getAddress() == null || source.getAddress().getHouseNumber() == null) {
            return null;
        }
        String houseNumberWithSuffix = source.getAddress().getHouseNumber().toString();
        if (source.getAddress().getHouseNumberSuffix() != null) {
            houseNumberWithSuffix += source.getAddress().getHouseNumberSuffix();
        }
        return houseNumberWithSuffix;
    }

    /**
     * Maps address fields from domain model to entity (i.e., splits house number strings).
     *
     * @param source the domain model containing address data; must not be null
     * @param addressEntity the AddressEntity to populate; must not be null
     * @return the populated AddressEntity
     */
    @SuppressWarnings("unused")
    default AddressEntity splitHouseNumber(Pos source, AddressEntity addressEntity) {
        if (addressEntity == null) {
            addressEntity = new AddressEntity();
        }
        if (source == null) {
            return addressEntity;
        }

        addressEntity.setStreet(source.street());
        addressEntity.setCity(source.city());
        addressEntity.setPostalCode(source.postalCode());

        // Parse house number and suffix
        if (source.houseNumber().isEmpty()) {
            return addressEntity;
        }
        String numericPart = source.houseNumber().replaceAll("[^0-9]", "");
        String suffixPart = source.houseNumber().replaceAll("[0-9]", "");
        if (!numericPart.isEmpty()) {
            addressEntity.setHouseNumber(Integer.parseInt(numericPart));
        } else {
            addressEntity.setHouseNumber(null);
        }
        if (!suffixPart.isEmpty()) {
            addressEntity.setHouseNumberSuffix(suffixPart.charAt(0));
        } else {
            addressEntity.setHouseNumberSuffix(null);
        }

        return addressEntity;
    }
}
