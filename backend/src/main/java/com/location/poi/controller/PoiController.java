package com.location.poi.controller;

import com.location.poi.dto.PoiDto;
import com.location.poi.service.interfaces.OverpassService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pois")
public class PoiController {
    private final OverpassService overpassService;
    private static final Logger log = LoggerFactory.getLogger(PoiController.class);

    public PoiController(OverpassService overpassService) {
        this.overpassService = overpassService;
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<PoiDto>> nearby(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon,
            @RequestParam(value = "radius", required = false, defaultValue = "300") int radius,
            @RequestParam(value = "categories", required = false, defaultValue = "fuel,restaurant,shopping_mall") String categoriesCsv
    ) {
        List<String> categories = Arrays.stream(categoriesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        try {
            List<PoiDto> pois = overpassService.searchNearby(lat, lon, radius, categories);
            return ResponseEntity.ok(pois);
        } catch (IllegalStateException ex) {
            log.warn("nearby upstream unavailable");
            return ResponseEntity.status(503).body(List.of());
        }
    }
}

