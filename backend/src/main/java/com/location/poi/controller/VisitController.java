package com.location.poi.controller;

import com.location.poi.dto.VisitEventResponse;
import com.location.poi.entity.UserEntity;
import com.location.poi.repository.UserRepository;
import com.location.poi.service.interfaces.VisitService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/visits")
public class VisitController {
    private final VisitService visitService;
    private final UserRepository userRepo;
    public VisitController(VisitService visitService, UserRepository userRepo) { this.visitService = visitService; this.userRepo = userRepo; }

    @GetMapping("/recent")
    public ResponseEntity<List<VisitEventResponse>> recent(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {
        
        Long userId = null;
        if (userDetails != null) {
            userId = userRepo.findByUsername(userDetails.getUsername())
                    .map(UserEntity::getId)
                    .orElse(null);
        }
        
        if (userId == null && userIdHeader != null) {
            userId = userIdHeader;
        }

        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        return ResponseEntity.ok(visitService.recent(userId, limit));
    }
}
