package de.seuhd.campuscoffee.domain.model;

/**
 * Enum for the different POS types.
 */
public enum PosType {
    CAFE,
    VENDING_MACHINE,
    BAKERY, // e.g., bakeries in grocery stores selling coffee
    CAFETERIA, // e.g., venues operated by the Studierendenwerk/Mensa
    OTHER // Fallback Typ bei unbekanntem OSM amenity/shop
}
