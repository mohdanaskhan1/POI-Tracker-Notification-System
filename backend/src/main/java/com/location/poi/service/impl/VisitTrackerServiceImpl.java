package com.location.poi.service.impl;

import com.location.poi.dto.PoiDto;
import com.location.poi.service.interfaces.VisitTrackerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VisitTrackerServiceImpl implements VisitTrackerService {
    private static final Logger log = LoggerFactory.getLogger(VisitTrackerServiceImpl.class);
    private static class DeviceState {
        final String poiId;
        final boolean entered;
        DeviceState(String poiId, boolean entered) { this.poiId = poiId; this.entered = entered; }
    }
    private final Map<String, DeviceState> lastStateByUserDevice = new ConcurrentHashMap<>();

    public boolean hasStateChanged(Long userId, String deviceId, PoiDto poi, boolean isEntered) {
        String userDeviceKey = userId + ":" + deviceId;
        synchronized (userDeviceKey.intern()) {
            String currentPoiId = poi != null ? poi.getId() : null;
            DeviceState prev = lastStateByUserDevice.get(userDeviceKey);

            if (currentPoiId == null) {
                if (prev != null) {
                    log.info("User {} Device {}: LEFT location (was {})", userId, deviceId, prev.poiId);
                    lastStateByUserDevice.remove(userDeviceKey);
                    return true;
                }
                return false;
            }

            boolean wasNotEntered = prev == null || !currentPoiId.equals(prev.poiId) || !prev.entered;

            if (isEntered && wasNotEntered) {
                log.info("User {} Device {}: ENTERED {} ({})", userId, deviceId, poi.getName(), currentPoiId);
                lastStateByUserDevice.put(userDeviceKey, new DeviceState(currentPoiId, true));
                return true;
            }

            if (!isEntered) {
                if (prev == null || !currentPoiId.equals(prev.poiId) || prev.entered) {
                    log.info("User {} Device {}: NEARBY {} ({})", userId, deviceId, poi.getName(), currentPoiId);
                    lastStateByUserDevice.put(userDeviceKey, new DeviceState(currentPoiId, false));
                    return true;
                }
            }

            return false;
        }
    }
}
