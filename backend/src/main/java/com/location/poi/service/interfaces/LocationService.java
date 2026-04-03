package com.location.poi.service.interfaces;

import com.location.poi.dto.LocationReportRequest;
import com.location.poi.dto.VisitEventResponse;

public interface LocationService {
    VisitEventResponse handleLocationReport(Long userId, LocationReportRequest report);
}
