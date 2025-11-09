package de.seuhd.campuscoffee.systest;

import de.seuhd.campuscoffee.api.dtos.PosDto;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = PosOsmImportSystemTests.OsmStubConfig.class)
public class PosOsmImportSystemTests extends AbstractSysTest {

    private static final long NODE_OK = 1001L;
    private static final long NODE_NOT_FOUND = 1002L;
    private static final long NODE_MISSING_FIELDS = 1003L;
    private static final long NODE_FALLBACK_TYPE = 1004L;

    @TestConfiguration
    static class OsmStubConfig {
        @Bean
        @Primary
        OsmDataService osmDataService() {
            return new FakeOsmDataService();
        }
    }

    static class FakeOsmDataService implements OsmDataService {
        private final Map<Long, OsmNode> nodes = new HashMap<>();

        FakeOsmDataService() {
            // Happy path node with typical cafe attributes
            nodes.put(NODE_OK, OsmNode.builder()
                    .nodeId(NODE_OK)
                    .lat(49.41).lon(8.70)
                    .tags(Map.of(
                            "name", "Stub Kaffee",
                            "amenity", "cafe",
                            "addr:street", "Hauptstraße",
                            "addr:housenumber", "100",
                            "addr:postcode", "69117",
                            "addr:city", "Heidelberg",
                            "description", "Imported from stub"
                    ))
                    .build());

            // Missing fields: no name
            nodes.put(NODE_MISSING_FIELDS, OsmNode.builder()
                    .nodeId(NODE_MISSING_FIELDS)
                    .lat(49.41).lon(8.70)
                    .tags(Map.of(
                            "amenity", "cafe",
                            "addr:street", "Hauptstraße",
                            "addr:housenumber", "101",
                            "addr:postcode", "69117",
                            "addr:city", "Heidelberg"
                    ))
                    .build());

            // Fallback type: unknown amenity
            nodes.put(NODE_FALLBACK_TYPE, OsmNode.builder()
                    .nodeId(NODE_FALLBACK_TYPE)
                    .lat(49.41).lon(8.70)
                    .tags(Map.of(
                            "name", "Some Place",
                            "amenity", "unknown",
                            "addr:street", "Nebenstraße",
                            "addr:housenumber", "5a",
                            "addr:postcode", "69120",
                            "addr:city", "Heidelberg"
                    ))
                    .build());
        }

        @Override
        public @NonNull OsmNode fetchNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
            if (nodeId.equals(NODE_NOT_FOUND)) {
                throw new OsmNodeNotFoundException(nodeId);
            }
            OsmNode node = nodes.get(nodeId);
            if (node == null) {
                throw new OsmNodeNotFoundException(nodeId);
            }
            return node;
        }
    }

    @BeforeEach
    void setup() {
        // posService.clear() bereits in AbstractSysTest
    }

    @AfterEach
    void tearDown() {
        // posService.clear() bereits in AbstractSysTest
    }

    @Test
    void importFromOsm_HappyPath_Returns201AndPersists() {
        PosDto created = given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/pos/import/osm/{nodeId}", NODE_OK)
                .then()
                .statusCode(201)
                .extract().as(PosDto.class);

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Stub Kaffee");
        assertThat(created.type()).isEqualTo(PosType.CAFE);
        assertThat(created.street()).isEqualTo("Hauptstraße");
        assertThat(created.houseNumber()).isEqualTo("100");
        assertThat(created.postalCode()).isEqualTo(69117);
        assertThat(created.city()).isEqualTo("Heidelberg");

        // anschließend GET verifizieren
        PosDto retrieved = given()
                .when()
                .get("/api/pos/{id}", created.id())
                .then()
                .statusCode(200)
                .extract().as(PosDto.class);
        assertThat(retrieved).usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(created);
    }

    @Test
    void importFromOsm_NodeNotFound_Returns404() {
        given()
                .when()
                .post("/api/pos/import/osm/{nodeId}", NODE_NOT_FOUND)
                .then()
                .statusCode(404);
    }

    @Test
    void importFromOsm_MissingRequiredFields_Returns400() {
        given()
                .when()
                .post("/api/pos/import/osm/{nodeId}", NODE_MISSING_FIELDS)
                .then()
                .statusCode(400);
    }

    @Test
    void importFromOsm_UnknownAmenity_FallbackToOther() {
        PosDto created = given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/pos/import/osm/{nodeId}", NODE_FALLBACK_TYPE)
                .then()
                .statusCode(201)
                .extract().as(PosDto.class);

        assertThat(created.type()).isEqualTo(PosType.OTHER);
    }

    @Test
    void importFromOsm_Idempotent_ReturnsSamePos() {
        PosDto first = given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/pos/import/osm/{nodeId}", NODE_OK)
                .then()
                .statusCode(201)
                .extract().as(PosDto.class);

        PosDto second = given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/pos/import/osm/{nodeId}", NODE_OK)
                .then()
                .statusCode(201)
                .extract().as(PosDto.class);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second).usingRecursiveComparison().ignoringFields("createdAt", "updatedAt").isEqualTo(first);
    }
}
