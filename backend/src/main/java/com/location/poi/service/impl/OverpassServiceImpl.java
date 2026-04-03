package com.location.poi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.location.poi.dto.PoiDto;
import com.location.poi.service.interfaces.OverpassService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.time.Duration;

@Service
public class OverpassServiceImpl implements OverpassService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    @Value("${overpass.urls:https://overpass-api.de/api/interpreter,https://lz4.overpass-api.de/api/interpreter,https://overpass.kumi.systems/api/interpreter}")
    private String overpassUrls;
    private static final Logger log = LoggerFactory.getLogger(OverpassServiceImpl.class);

    public List<PoiDto> searchNearby(double lat, double lon, int radiusMeters, List<String> categories) {
        log.debug("overpass request lat={} lon={} radius={} categories={}", lat, lon, radiusMeters, categories);
        
        String query = buildQuery(lat, lon, radiusMeters, categories);
        
        for (String endpoint : endpoints()) {
            try {
                long start = System.currentTimeMillis();
                List<PoiDto> pois = fetchFromEndpoint(endpoint, query, lat, lon);
                long took = System.currentTimeMillis() - start;
                if (pois != null) {
                    List<PoiDto> out = pois.stream()
                            .sorted(Comparator.comparingDouble(PoiDto::getDistanceMeters))
                            .limit(50)
                            .collect(Collectors.toList());
                    log.debug("overpass success from {} (took {}ms, {} results)", endpoint, took, out.size());
                    return out;
                }
            } catch (Exception ex) {
                log.warn("overpass endpoint error {}: {}", endpoint, ex.getMessage());
            }
        }
        
        log.warn("Overpass API unavailable or timed out after trying all endpoints.");
        throw new IllegalStateException("Overpass upstream unavailable");
    }

    private String buildQuery(double lat, double lon, int radius, List<String> categories) {
        Set<String> cats = categories == null || categories.isEmpty()
                ? Set.of("fuel", "restaurant", "shopping_mall")
                : categories.stream().map(String::toLowerCase).collect(Collectors.toCollection(LinkedHashSet::new));
        
        StringBuilder sb = new StringBuilder();
        sb.append("[out:json][timeout:25];(");
        
        for (String c : cats) {
            String filter;
            switch (c) {
                case "fuel":
                    filter = "[\"amenity\"=\"fuel\"]";
                    break;
                case "restaurant":
                    filter = "[\"amenity\"~\"restaurant|fast_food\"]";
                    break;
                case "shopping_mall":
                case "mall":
                case "shopping":
                    filter = "[\"shop\"=\"mall\"]";
                    break;
                default:
                    filter = "[\"amenity\"=\"" + c + "\"]";
            }
            sb.append("node").append(filter).append("(around:").append(radius).append(",").append(lat).append(",").append(lon).append(");");
            sb.append("way").append(filter).append("(around:").append(radius).append(",").append(lat).append(",").append(lon).append(");");
        }
        sb.append(");out center;");
        return sb.toString();
    }

    private List<PoiDto> fetchFromEndpoint(String endpoint, String query, double lat, double lon) throws Exception {
        String body = "data=" + encode(query);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("overpass status={}", response.statusCode());
        if (response.statusCode() != 200) {
            log.warn("overpass non-200 status={}", response.statusCode());
            return null;
        }
        JsonNode root = mapper.readTree(response.body());
        JsonNode elements = root.get("elements");
        if (elements == null || !elements.isArray()) {
            log.warn("overpass elements missing");
            return null;
        }
        List<PoiDto> pois = new ArrayList<>();
        for (JsonNode e : elements) {
            String id = e.has("id") ? e.get("id").asText() : UUID.randomUUID().toString();
            double plat;
            double plon;
            if (e.has("lat") && e.has("lon")) {
                plat = e.get("lat").asDouble();
                plon = e.get("lon").asDouble();
            } else if (e.has("center")) {
                JsonNode c = e.get("center");
                plat = c.get("lat").asDouble();
                plon = c.get("lon").asDouble();
            } else {
                continue;
            }
            JsonNode tags = e.get("tags");
            String name = tags != null && tags.has("name") ? tags.get("name").asText() : "Unknown";
            String category = detectCategory(tags);
            double distance = haversineMeters(lat, lon, plat, plon);
            pois.add(new PoiDto(id, name, category, plat, plon, distance));
        }
        return pois;
    }

    private List<String> endpoints() {
        if (overpassUrls == null || overpassUrls.isBlank()) {
            return List.of("https://overpass-api.de/api/interpreter");
        }
        return Arrays.stream(overpassUrls.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String detectCategory(JsonNode tags) {
        if (tags == null) return "unknown";
        if (tags.has("amenity")) {
            String a = tags.get("amenity").asText();
            if (a.equals("fuel")) return "fuel";
            if (a.equals("restaurant")) return "restaurant";
            if (a.equals("fast_food")) return "restaurant";
        }
        if (tags.has("shop")) {
            String s = tags.get("shop").asText();
            if (s.equals("mall")) return "shopping_mall";
        }
        return "unknown";
    }

    private static String encode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
