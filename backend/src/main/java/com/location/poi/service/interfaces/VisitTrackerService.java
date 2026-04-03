package com.location.poi.service.interfaces;

import com.location.poi.dto.PoiDto;

public interface VisitTrackerService {
    boolean hasStateChanged(Long userId, String deviceId, PoiDto poi, boolean isEntered);
}
