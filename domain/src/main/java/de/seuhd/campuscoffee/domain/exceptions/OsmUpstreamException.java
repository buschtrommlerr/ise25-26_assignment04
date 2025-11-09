package de.seuhd.campuscoffee.domain.exceptions;

/**
 * Exception für Fehler des externen OSM-Dienstes (Upstream), z.B. 5xx Statuscodes oder nicht parsebare Antworten.
 */
public class OsmUpstreamException extends RuntimeException {
    public OsmUpstreamException(String message) {
        super(message);
    }

    public OsmUpstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}

