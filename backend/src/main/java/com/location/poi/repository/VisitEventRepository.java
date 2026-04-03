package com.location.poi.repository;

import com.location.poi.entity.VisitEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VisitEventRepository extends JpaRepository<VisitEventEntity, Long> {
    Page<VisitEventEntity> findAllByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
    Optional<VisitEventEntity> findTopByUserIdAndDeviceIdOrderByTimestampDesc(Long userId, String deviceId);
    Optional<VisitEventEntity> findTopByUserIdOrderByTimestampDesc(Long userId);
}
