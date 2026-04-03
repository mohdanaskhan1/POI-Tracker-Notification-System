package com.location.poi.service.interfaces;

import com.location.poi.dto.PoiDto;
import com.location.poi.dto.VisitEventResponse;
import java.util.List;

public interface VisitService {
    void processVisitEvent(Long userId, String deviceId, PoiDto nearestPoi, boolean isEntered);
    boolean recordVisit(Long userId, String deviceId, PoiDto poi, boolean entered);
    List<VisitEventResponse> recent(Long userId, int limit);
}
