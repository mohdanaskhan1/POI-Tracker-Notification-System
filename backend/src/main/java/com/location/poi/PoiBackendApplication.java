package com.location.poi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "POI Detection API", version = "1.0", description = "Location-Based POI Detection System Backend"))
public class PoiBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(PoiBackendApplication.class, args);
    }
}

