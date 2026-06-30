package com.internhub.controller;

import com.internhub.dto.AuthResponse;
import com.internhub.dto.LoginRequest;
import com.internhub.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/mock-login")
    public AuthResponse mockLogin(@Valid @RequestBody LoginRequest request) {
        return authService.mockLogin(request);
    }
}
