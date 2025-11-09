package de.seuhd.campuscoffee.domain.model;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Represents an OpenStreetMap node with relevant Point of Sale information.
 * This is the domain model for OSM data before it is converted to a POS object.
 *
 * @param nodeId The OpenStreetMap node ID.
 * @param lat Latitude of the node (optional).
 * @param lon Longitude of the node (optional).
 * @param tags Key-value pairs from {@code <tag k="..." v="..."/>} elements.
 */
@Builder
public record OsmNode(@NonNull Long nodeId,
                      @Nullable Double lat,
                      @Nullable Double lon,
                      @NonNull Map<String, String> tags) {
}
