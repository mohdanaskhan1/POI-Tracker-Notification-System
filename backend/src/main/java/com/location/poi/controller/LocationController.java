package com.location.poi.controller;

import com.location.poi.dto.LocationReportRequest;
import com.location.poi.dto.VisitEventResponse;
import com.location.poi.entity.UserEntity;
import com.location.poi.repository.UserRepository;
import com.location.poi.service.interfaces.LocationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locations")
public class LocationController {
    private static final Logger log = LoggerFactory.getLogger(LocationController.class);
    private final LocationService locationService;
    private final UserRepository userRepo; // To fetch user ID from username

    public LocationController(LocationService locationService, UserRepository userRepo) {
        this.locationService = locationService;
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

        return ResponseEntity.ok(locationService.handleLocationReport(userId, report));
    }
}
