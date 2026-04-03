package com.location.poi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LocationReportRequest {
    @NotBlank
    private String deviceId;
    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;
    private Long timestamp;
    private Integer radius;
    private String categories;
}
