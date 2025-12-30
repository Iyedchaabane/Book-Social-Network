package com.ichaabane.book_network.presentation.controller;

import com.ichaabane.book_network.application.dto.request.AuthenticationRequest;
import com.ichaabane.book_network.application.dto.request.CodeVerificationRequest;
import com.ichaabane.book_network.application.dto.request.RegistrationRequest;
import com.ichaabane.book_network.application.dto.request.ResetPasswordRequest;
import com.ichaabane.book_network.application.dto.response.AuthenticationResponse;
import com.ichaabane.book_network.application.service.AuthenticationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<?> register(@RequestBody @Valid RegistrationRequest registrationRequest) throws MessagingException {
        authenticationService.register(registrationRequest);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/authenticate")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PostMapping("/activate-account")
    public ResponseEntity<Map<String, String>> activateAccount(@RequestBody CodeVerificationRequest request) throws MessagingException {
        authenticationService.activateAccount(request.getToken());
        return ResponseEntity.ok(Map.of("message","Account activated"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) throws MessagingException {
        authenticationService.forgotPassword(request.get("email"));
        return ResponseEntity.ok(Map.of("message","Reset password instructions sent to your email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest request) throws MessagingException {
        authenticationService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message","Password reset successful"));
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<Map<String, String>> verifyResetCode(@RequestBody CodeVerificationRequest request) {
        authenticationService.verifyResetToken(request.getToken());
        return ResponseEntity.ok(Map.of("message","Code verified"));
    }
}
