package com.location.poi.service.interfaces;

import com.location.poi.dto.LoginRequest;
import com.location.poi.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> register(RegisterRequest request);
    ResponseEntity<?> login(LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response);
}
