package com.booking.user.web.controller;

import com.booking.common.dto.ApiResponse;
import com.booking.user.application.dto.LoginRequest;
import com.booking.user.application.dto.LogoutRequest;
import com.booking.user.application.dto.RefreshRequest;
import com.booking.user.application.dto.RegisterRequest;
import com.booking.user.application.dto.TokenResponse;
import com.booking.user.application.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints — all public (no JWT required).
 *
 * <pre>
 *  POST /v1/auth/register  — create account + issue token pair
 *  POST /v1/auth/login     — authenticate + issue token pair
 *  POST /v1/auth/refresh   — rotate tokens
 *  POST /v1/auth/logout    — blacklist access token + delete refresh token
 * </pre>
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh and logout")
public class AuthController {

    private final UserService userService;

    @Operation(summary = "Register a new customer account")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Account created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email or phone already registered"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation failed")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(
            @Valid @RequestBody RegisterRequest req) {

        TokenResponse tokens = userService.register(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", tokens));
    }

    @Operation(summary = "Login with email and password")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest req) {

        TokenResponse tokens = userService.login(req);
        return ResponseEntity.ok(ApiResponse.success("Login successful", tokens));
    }

    @Operation(summary = "Exchange a valid refresh token for a new token pair")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tokens rotated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh token invalid or expired")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest req) {

        TokenResponse tokens = userService.refresh(req);
        return ResponseEntity.ok(ApiResponse.success("Tokens refreshed", tokens));
    }

    @Operation(summary = "Logout — blacklist access token and revoke refresh token")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest req) {

        userService.logout(req);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
}
