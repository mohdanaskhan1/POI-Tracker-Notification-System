package com.location.poi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiDto {
    private String id;
    private String name;
    private String category;
    private double latitude;
    private double longitude;
    private double distanceMeters;
}
