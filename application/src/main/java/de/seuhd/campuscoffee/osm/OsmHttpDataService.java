package de.seuhd.campuscoffee.osm;

import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.exceptions.OsmUpstreamException;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Real HTTP-based implementation of the OsmDataService that calls the OpenStreetMap API.
 * This adapter lives in the application module and overrides the stub in the data module.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OsmHttpDataService implements OsmDataService {

    @Value("${osm.base-url:https://www.openstreetmap.org}")
    private String baseUrl;

    private final RestClient.Builder restClientBuilder;

    @Override
    public @NonNull OsmNode fetchNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        RestClient client = restClientBuilder.baseUrl(baseUrl).build();
        String path = "/api/0.6/node/" + nodeId;
        try {
            String body = client.get()
                    .uri(path)
                    .accept(MediaType.APPLICATION_XML)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new OsmNodeNotFoundException(nodeId);
                        }
                        throw new OsmUpstreamException("OSM client error: " + response.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new OsmUpstreamException("OSM upstream error: " + response.getStatusCode());
                    })
                    .body(String.class);

            return parseOsmNode(nodeId, body);
        } catch (OsmNodeNotFoundException e) {
            throw e;
        } catch (OsmUpstreamException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch OSM node {}", nodeId, e);
            throw new OsmUpstreamException("Failed to fetch OSM node " + nodeId, e);
        }
    }

    private OsmNode parseOsmNode(Long expectedNodeId, String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            NodeList nodes = doc.getElementsByTagName("node");
            if (nodes.getLength() == 0) {
                throw new OsmUpstreamException("OSM response contained no <node> element");
            }
            Element nodeEl = (Element) nodes.item(0);
            Long nodeId = Long.parseLong(nodeEl.getAttribute("id"));
            if (!nodeId.equals(expectedNodeId)) {
                log.warn("OSM response node id {} doesn't match requested {}", nodeId, expectedNodeId);
            }
            Double lat = nodeEl.hasAttribute("lat") ? Double.parseDouble(nodeEl.getAttribute("lat")) : null;
            Double lon = nodeEl.hasAttribute("lon") ? Double.parseDouble(nodeEl.getAttribute("lon")) : null;

            Map<String, String> tags = new HashMap<>();
            NodeList tagEls = nodeEl.getElementsByTagName("tag");
            for (int i = 0; i < tagEls.getLength(); i++) {
                Element tag = (Element) tagEls.item(i);
                String k = tag.getAttribute("k");
                String v = tag.getAttribute("v");
                if (k != null) tags.put(k, v);
            }
            return OsmNode.builder()
                    .nodeId(nodeId)
                    .lat(lat)
                    .lon(lon)
                    .tags(tags)
                    .build();
        } catch (Exception e) {
            throw new OsmUpstreamException("Failed to parse OSM XML for node " + expectedNodeId, e);
        }
    }
}

