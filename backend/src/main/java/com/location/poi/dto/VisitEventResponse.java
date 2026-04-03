package com.location.poi.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitEventResponse {
    private boolean entered;
    private PoiDto poi;
    private String deviceId;
}
