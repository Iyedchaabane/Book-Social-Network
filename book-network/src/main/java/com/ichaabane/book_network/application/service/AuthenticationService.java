package com.ichaabane.book_network.application.service;

import com.ichaabane.book_network.application.dto.request.AuthenticationRequest;
import com.ichaabane.book_network.application.dto.request.RegistrationRequest;
import com.ichaabane.book_network.application.dto.request.ResetPasswordRequest;
import com.ichaabane.book_network.application.dto.request.UserRequest;
import com.ichaabane.book_network.application.dto.response.AuthenticationResponse;
import com.ichaabane.book_network.domain.model.Token;
import com.ichaabane.book_network.domain.model.User;
import com.ichaabane.book_network.domain.enums.TokenType.*;
import com.ichaabane.book_network.domain.repository.TokenRepository;
import com.ichaabane.book_network.domain.repository.UserRepository;
import com.ichaabane.book_network.infrastructure.email.EmailTemplateName;
import com.ichaabane.book_network.domain.exception.*;
import com.ichaabane.book_network.domain.repository.RoleRepository;
import com.ichaabane.book_network.infrastructure.security.JwtService;
import jakarta.annotation.PostConstruct;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import static com.ichaabane.book_network.domain.enums.TokenType.*;
import com.ichaabane.book_network.infrastructure.email.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final TokenRepository tokenRepository;
    private final TokenService tokenService;
    private AuthenticationService self;

    @PostConstruct
    private void init() {
        self = this;
    }


    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;

    @Value("${application.mailing.frontend.reset-url}")
    private String resetUrl;

    @Value("${application.mailing.frontend.add-url}")
    private String addUrl;

//    @Transactional
    public void register(RegistrationRequest registrationRequest) {
        if (userRepository.existsByEmail(registrationRequest.getEmail())) {
            throw new OperationNotPermittedException("The email address already exists. Please use a different one.");
        }
        var userRole = roleRepository.findByName("USER")
                .orElseThrow( () -> {
                    log.error("USER role not found in the database");
                    return new IllegalStateException("User Role Not Found");
                });
        User user = User
                .builder()
                .firstName(registrationRequest.getFirstname())
                .lastName(registrationRequest.getLastname())
                .email(registrationRequest.getEmail())
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .roles(List.of(userRole))
                .build();

        userRepository.save(user);
        log.info("Registration successful for user: {}", user.getEmail());
        sendValidationEmail(user);
    }

    @Transactional
    public void activateAccount(String code) {
        var validToken = tokenRepository.findFirstByTokenAndTypeOrderByCreatedAtDesc(code, ACCOUNT_ACTIVATION)
                .orElseThrow(() -> {
                    log.warn("Invalid activation token: {}", code);
                    return new InvalidTokenException("Invalid activation token");
                });

        if (LocalDateTime.now().isAfter(validToken.getExpiresAt())) {
            log.warn("Expired activation token for {}. Sending new token", validToken.getUser().getEmail());
            self.sendValidationEmail(validToken.getUser());
            throw new ExpiredTokenException("Token expired . A new token has been send to the same email");
        }

        var user = userRepository.findById(validToken.getUser().getId())
                .orElseThrow(() -> {
                    log.warn("No user found with Id: {}", validToken.getUser().getId());
                    return new UsernameNotFoundException("User not found");
                });

        user.setEnabled(true);
        userRepository.save(user);
        validToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(validToken);

        log.info("Account successfully activated for user: {}", user.getEmail());
    }

    public void sendValidationEmail(User user) {
        String newToken = null;
        try {
            newToken = tokenService.generateAndSaveActivationToken(user, ACCOUNT_ACTIVATION);
            emailService.sendEmail(
                    user.getEmail(),
                    user.getFullName(),
                    EmailTemplateName.ACTIVATE_ACCOUNT,
                    activationUrl,
                    newToken,
                    "Account activation"
            );
            log.info("Activation email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Email failed for {}", user.getEmail(), e);
            if (newToken != null) {
                tokenService.deleteToken(newToken); // Nettoyage compensatoire
            }
            throw e;
        }
    }


    public AuthenticationResponse authenticate(AuthenticationRequest request) {

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var claims = new HashMap<String, Object>();
        var user = ((User) authentication.getPrincipal());
        claims.put("fullName", user.getFullName());
        claims.put("userId", user.getId());

        var jwtToken = jwtService.generateToken(claims, (User) authentication.getPrincipal());

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("No user found with email: {}", email);
                    return new UsernameNotFoundException("User not found");
                });

        String token = tokenService.generateAndSaveActivationToken(user, FORGOT_PASSWORD);

        emailService.sendEmail(
                user.getEmail(),
                user.getFullName(),
                EmailTemplateName.FORGOT_PASSWORD,
                resetUrl,
                token,
                "Password reset request"
        );

        log.info("Password reset email sent to {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("Password mismatch for token: {}", request.getToken());
            throw new PasswordMismatchException("Passwords do not match");
        }

        Token validToken = tokenRepository.findFirstByTokenAndTypeOrderByCreatedAtDesc(request.getToken(), FORGOT_PASSWORD)
                .orElseThrow(() -> {
                    log.warn("Invalid password reset token: {}", request.getToken());
                    return new InvalidTokenException("Invalid reset token");
                });

        if (LocalDateTime.now().isAfter(validToken.getExpiresAt())) {
            log.warn("Expired password reset token for user: {}", validToken.getUser().getEmail());
            throw new ExpiredTokenException("Token expired");
        }

        if (validToken.getValidatedAt() == null) {
            log.warn("Unverified reset code for user: {}", validToken.getUser().getEmail());
            throw new CodeNotVerifiedException("Code not verified");
        }

        User user = validToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password successfully updated for user: {}", user.getEmail());
    }

    @Transactional
    public void verifyResetToken(String otp) {
        Token resetToken = tokenRepository.findFirstByTokenAndTypeOrderByCreatedAtDesc(otp, FORGOT_PASSWORD)
                .orElseThrow(() -> {
                    log.warn("Invalid verification token: {}", otp);
                    return new InvalidTokenException("Invalid reset token");
                });

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Expired verification token: {}", otp);
            throw new ExpiredTokenException("Token expired");
        }

        resetToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);

        log.info("Reset code successfully verified for user: {}", resetToken.getUser().getEmail());
    }

    public Integer createUser(UserRequest request) {
        var userRole = roleRepository.findByName("USER")
                .orElseThrow( () -> {
                    log.error("USER role not found in the database");
                    return new IllegalStateException("User Role Not Found");
                });
        User user = User
                .builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .dateOfBirth(request.dateOfBirth())
                .password(passwordEncoder.encode(""))
                .accountLocked(false)
                .enabled(true)
                .roles(List.of(userRole))
                .build();

        userRepository.save(user);
        log.info("Registration successful for user: {}", user.getEmail());
        // 3. Send password creation link using forgot-password logic
        String token = tokenService.generateAndSaveActivationToken(user, SET_PASSWORD);

        emailService.sendEmail(
                user.getEmail(),
                user.getFullName(),
                EmailTemplateName.SET_PASSWORD, // or custom template "SET_PASSWORD"
                addUrl,
                token,
                "Set your password"
        );

        log.info("User created by admin: {} and password setup email sent", user.getEmail());
        return user.getId();
    }
}
