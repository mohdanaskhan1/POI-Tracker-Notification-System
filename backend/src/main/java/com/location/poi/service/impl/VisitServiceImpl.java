package com.location.poi.service.impl;

import com.location.poi.dto.PoiDto;
import com.location.poi.dto.VisitEventResponse;
import com.location.poi.entity.VisitEventEntity;
import com.location.poi.entity.UserEntity;
import com.location.poi.repository.UserRepository;
import com.location.poi.repository.VisitEventRepository;
import com.location.poi.service.interfaces.VisitService;
import com.location.poi.service.interfaces.VisitTrackerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class VisitServiceImpl implements VisitService {
    private static final Logger log = LoggerFactory.getLogger(VisitServiceImpl.class);
    private final VisitEventRepository visitRepo;
    private final VisitTrackerService visitTracker;
    private final UserRepository userRepo;

    public VisitServiceImpl(VisitEventRepository visitRepo, VisitTrackerService visitTracker, UserRepository userRepo) {
        this.visitRepo = visitRepo;
        this.visitTracker = visitTracker;
        this.userRepo = userRepo;
    }

    public void processVisitEvent(Long userId, String deviceId, PoiDto nearestPoi, boolean isEntered) {
        String userDeviceKey = userId + ":" + deviceId;
        synchronized (userDeviceKey.intern()) {
            boolean significantChange = visitTracker.hasStateChanged(userId, deviceId, nearestPoi, isEntered);
            if (significantChange && isEntered) {
                recordVisit(userId, deviceId, nearestPoi, true);
            }
        }
    }

    public boolean recordVisit(Long userId, String deviceId, PoiDto poi, boolean entered) {
        log.debug("Recording visit event for user {} device {}: poi={}, entered={}", userId, deviceId, poi != null ? poi.getName() : "null", entered);
        
        String userDeviceKey = userId + ":" + deviceId;
        synchronized (userDeviceKey.intern()) {
            var lastEventOpt = visitRepo.findTopByUserIdAndDeviceIdOrderByTimestampDesc(userId, deviceId);
            if (lastEventOpt.isPresent()) {
                var last = lastEventOpt.get();
                long timeSinceLastEvent = System.currentTimeMillis() - last.getTimestamp();
                String lastPoiId = last.getPoiId();
                String currentPoiId = poi != null ? poi.getId() : null;
                if (lastPoiId != null && currentPoiId != null && lastPoiId.equals(currentPoiId) && timeSinceLastEvent < 60_000) {
                    log.info("User {} Device {}: Same-POI cooldown active ({}ms), skipping duplicate entry for {}", 
                        userId, deviceId, timeSinceLastEvent, poi.getName());
                    return false;
                }
            }

            UserEntity user = userRepo.getReferenceById(userId);
            VisitEventEntity e = new VisitEventEntity();
            e.setUser(user);
            e.setDeviceId(deviceId);
            if (poi != null) {
                e.setPoiId(poi.getId());
                e.setPoiName(poi.getName());
                e.setCategory(poi.getCategory());
                e.setLatitude(poi.getLatitude());
                e.setLongitude(poi.getLongitude());
                e.setDistanceMeters(poi.getDistanceMeters());
            }
            e.setTimestamp(System.currentTimeMillis());
            e.setEntered(entered);
            visitRepo.save(e);
            return true;
        }
    }

    public List<VisitEventResponse> recent(Long userId, int limit) {
        log.debug("Fetching recent visits for user {}, limit={}", userId, limit);
        return visitRepo.findAllByUserIdOrderByTimestampDesc(userId, PageRequest.of(0, Math.max(1, Math.min(100, limit))))
                .getContent()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private VisitEventResponse convertToDto(VisitEventEntity entity) {
        VisitEventResponse dto = new VisitEventResponse();
        dto.setDeviceId(entity.getDeviceId());
        dto.setEntered(Boolean.TRUE.equals(entity.getEntered()));
        if (entity.getPoiId() != null) {
            PoiDto p = new PoiDto(entity.getPoiId(), entity.getPoiName(), entity.getCategory(),
                    entity.getLatitude() != null ? entity.getLatitude() : 0.0,
                    entity.getLongitude() != null ? entity.getLongitude() : 0.0,
                    entity.getDistanceMeters() != null ? entity.getDistanceMeters() : 0.0);
            dto.setPoi(p);
        }
        return dto;
    }
}
