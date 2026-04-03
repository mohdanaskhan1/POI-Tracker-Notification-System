package com.location.poi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "visit_events", indexes = {
    @Index(name = "idx_visit_user_ts", columnList = "user_id, timestamp DESC"),
    @Index(name = "idx_visit_device", columnList = "deviceId")
})
@Getter
@Setter
@NoArgsConstructor
public class VisitEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 100)
    private String deviceId;
    @Column(length = 100)
    private String poiId;
    @Column(length = 255)
    private String poiName;
    @Column(length = 100)
    private String category;
    private Double latitude;
    private Double longitude;
    private Double distanceMeters;
    private Long timestamp;
    private Boolean entered;
}
