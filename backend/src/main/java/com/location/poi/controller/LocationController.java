package com.location.poi.controller;

import com.location.poi.dto.LocationReportRequest;
import com.location.poi.dto.PoiDto;
import com.location.poi.dto.VisitEventResponse;
import com.location.poi.entity.UserEntity;
import com.location.poi.repository.UserRepository;
import com.location.poi.service.interfaces.OverpassService;
import com.location.poi.service.interfaces.VisitService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/locations")
public class LocationController {
    private static final Logger log = LoggerFactory.getLogger(LocationController.class);
    private final OverpassService overpassService;
    private final VisitService visitService;
    private final UserRepository userRepo; // To fetch user ID from username
    private static final double ENTRY_THRESHOLD_METERS = 50.0;

    public LocationController(OverpassService overpassService, VisitService visitService, UserRepository userRepo) {
        this.overpassService = overpassService;
        this.visitService = visitService;
        this.userRepo = userRepo;
    }

    @PostMapping("/report")
    public ResponseEntity<VisitEventResponse> report(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @Valid @RequestBody LocationReportRequest report) {
        
        Long userId = null;
        if (userDetails != null) {
            userId = userRepo.findByUsername(userDetails.getUsername())
                    .map(UserEntity::getId)
                    .orElse(null);
        }
        
        if (userId == null && userIdHeader != null) {
            userId = userIdHeader;
        }

        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        int radius = report.getRadius() != null ? Math.max(50, Math.min(1000, report.getRadius())) : 300;
        String catsCsv = report.getCategories() != null ? report.getCategories() : "fuel,restaurant,shopping_mall";
        List<String> categories = Arrays.stream(catsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        
        List<PoiDto> pois;
        try {
            pois = overpassService.searchNearby(report.getLatitude(), report.getLongitude(), radius, categories);
        } catch (IllegalStateException ex) {
            pois = List.of();
        }

        // Find the absolute nearest POI
        PoiDto nearest = pois.stream()
                .min(java.util.Comparator.comparingDouble(PoiDto::getDistanceMeters))
                .orElse(null);

        // Strict entry threshold: 50m. Anything else is just 'nearby'.
        boolean isEntered = nearest != null && nearest.getDistanceMeters() <= ENTRY_THRESHOLD_METERS;
        
        // Record if necessary (logic handles state tracking and cooldown)
        visitService.processVisitEvent(userId, report.getDeviceId(), nearest, isEntered);
        
        VisitEventResponse ev = new VisitEventResponse();
        ev.setDeviceId(report.getDeviceId());
        ev.setEntered(isEntered);
        ev.setPoi(nearest);
        
        return ResponseEntity.ok(ev);
    }
}
