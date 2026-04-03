package com.location.poi.service.interfaces;

import com.location.poi.dto.PoiDto;
import java.util.List;

public interface OverpassService {
    List<PoiDto> searchNearby(double lat, double lon, int radiusMeters, List<String> categories);
}
