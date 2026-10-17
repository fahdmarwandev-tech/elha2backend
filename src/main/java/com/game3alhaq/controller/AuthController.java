package com.game3alhaq.controller;

import com.game3alhaq.dto.request.AuthRequests;
import com.game3alhaq.dto.response.AuthResponses;
import com.game3alhaq.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account with full name, phone number, optional email, and password.")
    public ResponseEntity<AuthResponses.AuthTokenResponse> register(@Valid @RequestBody AuthRequests.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in / Login", description = "Authenticate with phone number (or email) and password to get a JWT access token.")
    public ResponseEntity<AuthResponses.AuthTokenResponse> login(@Valid @RequestBody AuthRequests.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
