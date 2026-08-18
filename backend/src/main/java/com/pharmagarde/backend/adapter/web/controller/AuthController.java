package com.pharmagarde.backend.adapter.web.controller;

import com.pharmagarde.backend.application.dto.LoginRequest;
import com.pharmagarde.backend.application.dto.LoginResponse;
import com.pharmagarde.backend.application.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller d'authentification.
 * Se contente de transporter la requête HTTP vers AuthService —
 * aucune logique métier ici, conformément à la Clean Architecture.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}