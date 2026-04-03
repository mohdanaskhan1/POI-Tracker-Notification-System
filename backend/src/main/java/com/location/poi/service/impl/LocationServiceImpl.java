package com.location.poi.service.impl;

import com.location.poi.dto.LocationReportRequest;
import com.location.poi.dto.PoiDto;
import com.location.poi.dto.VisitEventResponse;
import com.location.poi.service.interfaces.LocationService;
import com.location.poi.service.interfaces.OverpassService;
import com.location.poi.service.interfaces.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationServiceImpl implements LocationService {
    private static final Logger log = LoggerFactory.getLogger(LocationServiceImpl.class);
    private final OverpassService overpassService;
    private final VisitService visitService;
    private static final double ENTRY_THRESHOLD_METERS = 50.0;

    public LocationServiceImpl(OverpassService overpassService, VisitService visitService) {
        this.overpassService = overpassService;
        this.visitService = visitService;
    }

    @Override
    public VisitEventResponse handleLocationReport(Long userId, LocationReportRequest report) {
        List<PoiDto> pois;
        try {
            pois = overpassService.searchNearby(report.getLatitude(), report.getLongitude(), report.getRadius(), report.getCategories());
        } catch (IllegalStateException ex) {
            log.warn("POI service unavailable: {}", ex.getMessage());
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
        
        return ev;
    }
}
