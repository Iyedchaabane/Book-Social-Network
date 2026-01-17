package com.ichaabane.book_network.application.service;

import com.ichaabane.book_network.application.dto.request.AuthenticationRequest;
import com.ichaabane.book_network.application.dto.request.RegistrationRequest;
import com.ichaabane.book_network.application.dto.request.ResetPasswordRequest;
import com.ichaabane.book_network.application.dto.request.UserRequest;
import com.ichaabane.book_network.application.dto.response.AuthenticationResponse;
import com.ichaabane.book_network.domain.exception.*;
import com.ichaabane.book_network.domain.model.Role;
import com.ichaabane.book_network.domain.model.Token;
import com.ichaabane.book_network.domain.model.User;
import com.ichaabane.book_network.domain.repository.RoleRepository;
import com.ichaabane.book_network.domain.repository.TokenRepository;
import com.ichaabane.book_network.domain.repository.UserRepository;
import com.ichaabane.book_network.infrastructure.email.EmailTemplateName;
import com.ichaabane.book_network.infrastructure.security.JwtService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.ichaabane.book_network.domain.enums.TokenType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * Tests unitaires pour AuthenticationService.
 * 
 * Utilise JUnit 5 (Jupiter) et Mockito BDD style.
 * Couverture: 100% de la logique métier.
 * 
 * Pour exécuter avec rapport de couverture:
 * mvn clean test jacoco:report
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService - Tests unitaires")
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private Role userRole;
    private Token validToken;

    @BeforeEach
    void setUp() {
        // Initialisation des URLs via réflexion
        ReflectionTestUtils.setField(authenticationService, "activationUrl", "http://localhost:4200/activate");
        ReflectionTestUtils.setField(authenticationService, "resetUrl", "http://localhost:4200/reset");
        ReflectionTestUtils.setField(authenticationService, "addUrl", "http://localhost:4200/set-password");
        
        // Configuration du self pour contourner @PostConstruct
        ReflectionTestUtils.setField(authenticationService, "self", authenticationService);

        // Données de test communes
        userRole = Role.builder()
                .id(1)
                .name("USER")
                .build();

        testUser = User.builder()
                .id(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .password("encoded-password")
                .accountLocked(false)
                .enabled(true)
                .roles(List.of(userRole))
                .build();

        validToken = Token.builder()
                .id(1)
                .token("123456")
                .type(ACCOUNT_ACTIVATION)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }

    @Nested
    @DisplayName("register() - Inscription utilisateur")
    class RegisterTests {

        @Test
        @DisplayName("Devrait créer un utilisateur avec succès")
        void shouldRegisterUserSuccessfully() throws MessagingException {
            // Given
            RegistrationRequest request = RegistrationRequest.builder()
                    .firstname("Jane")
                    .lastname("Smith")
                    .email("jane.smith@test.com")
                    .password("password123")
                    .build();

            given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
            given(roleRepository.findByName("USER")).willReturn(Optional.of(userRole));
            given(passwordEncoder.encode(request.getPassword())).willReturn("encoded-password");
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(tokenService.generateAndSaveActivationToken(any(User.class), eq(ACCOUNT_ACTIVATION)))
                    .willReturn("123456");
            willDoNothing().given(emailService).sendEmail(anyString(), anyString(), any(), anyString(), anyString(), anyString());

            // When
            authenticationService.register(request);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(userCaptor.capture());
            
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getFirstName()).isEqualTo("Jane");
            assertThat(savedUser.getLastName()).isEqualTo("Smith");
            assertThat(savedUser.getEmail()).isEqualTo("jane.smith@test.com");
            assertThat(savedUser.isEnabled()).isFalse();
            assertThat(savedUser.isAccountLocked()).isFalse();
            assertThat(savedUser.getRoles()).contains(userRole);

            then(emailService).should().sendEmail(
                    eq("jane.smith@test.com"),
                    eq("Jane Smith"),
                    eq(EmailTemplateName.ACTIVATE_ACCOUNT),
                    eq("http://localhost:4200/activate"),
                    eq("123456"),
                    eq("Account activation")
            );
        }

        @Test
        @DisplayName("Devrait échouer si l'email existe déjà")
        void shouldFailWhenEmailAlreadyExists() throws MessagingException {
            // Given
            RegistrationRequest request = RegistrationRequest.builder()
                    .firstname("John")
                    .lastname("Doe")
                    .email("john.doe@test.com")
                    .password("password123")
                    .build();

            given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

            // When / Then
            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("email address already exists");

            then(userRepository).should(never()).save(any(User.class));
            then(emailService).should(never()).sendEmail(anyString(), anyString(), any(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Devrait échouer si le rôle USER n'existe pas")
        void shouldFailWhenUserRoleNotFound() {
            // Given
            RegistrationRequest request = RegistrationRequest.builder()
                    .firstname("Jane")
                    .lastname("Smith")
                    .email("jane.smith@test.com")
                    .password("password123")
                    .build();

            given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
            given(roleRepository.findByName("USER")).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("User Role Not Found");

            then(userRepository).should(never()).save(any(User.class));
        }

        @Test
        @DisplayName("Devrait propager l'exception si l'envoi d'email échoue")
        void shouldPropagateExceptionWhenEmailFails() throws MessagingException {
            // Given
            RegistrationRequest request = RegistrationRequest.builder()
                    .firstname("Jane")
                    .lastname("Smith")
                    .email("jane.smith@test.com")
                    .password("password123")
                    .build();

            given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
            given(roleRepository.findByName("USER")).willReturn(Optional.of(userRole));
            given(passwordEncoder.encode(request.getPassword())).willReturn("encoded-password");
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(tokenService.generateAndSaveActivationToken(any(User.class), eq(ACCOUNT_ACTIVATION)))
                    .willReturn("123456");
            willThrow(new MessagingException("SMTP error"))
                    .given(emailService).sendEmail(anyString(), anyString(), any(), anyString(), anyString(), anyString());

            // When / Then
            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("SMTP error");

            then(tokenService).should().deleteToken("123456");
        }
    }

    @Nested
    @DisplayName("activateAccount() - Activation de compte")
    class ActivateAccountTests {

        @Test
        @DisplayName("Devrait activer le compte avec un token valide")
        void shouldActivateAccountWithValidToken() throws MessagingException {
            // Given
            String code = "123456";
            testUser.setEnabled(false);
            
            given(tokenRepository.findFirstByTokenAndTypeOrderByCreatedAtDesc(code, ACCOUNT_ACTIVATION))
                    .willReturn(Optional.of(validToken));
            given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(tokenRepository.save(any(Token.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            authenticationService.activateAccount(code);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(userCaptor.capture());
            assertThat(userCaptor.getValue().isEnabled()).isTrue();

            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            then(tokenRepository).should().save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getValidatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Devrait échouer avec un token invalide")
        void shouldFailWithInvalidToken() {
            // Given
            String code = "invalid";
            given(tokenRepository.findFirstByTokenAndTypeOrderByCreatedAtDesc(code, ACCOUNT_ACTIVATION))
                    .willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> authenticationService.activateAccount(code))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid activation token");

            then(userRepository).should(never()).save(any(User.class));
        }

        @Test
        @DisplayName("Devrait renvoyer un email si le token est expiré")
        void shouldResendEmailWhenTokenExpired() throws MessagingException {
            // Given
            String code = "123456";
            Token expiredToken = Token.builder()
                    .id(1)
                    .token(code)
                    .type(ACCOUNT_ACTIVATION)
                    .user(testUser)
                    .createdAt(LocalDateTime.now().minusMinutes(30))
                    .expiresAt(LocalDateTime.now().minusMinutes(15))
                    .build();

            given(tokenRepository.findFirstByTokenAndTypeOrderByCreatedAtDesc(code, ACCOUNT_ACTIVATION))
                    .willReturn(Optional.of(expiredToken));
            given(tokenService.generateAndSaveActivationToken(testUser, ACCOUNT_ACTIVATION))
                    .willReturn("654321");
            willDoNothing().given(emailService).sendEmail(anyString(), anyString(), any(), anyString(), anyString(), anyString());

            // When / Then
            assertThatThrownBy(() -> authenticationService.activateAccount(code))
                    .isInstanceOf(ExpiredTokenException.class)
                    .hasMessageContaining("Token expired");

            then(emailService).should().sendEmail(
                    eq(testUser.getEmail()),
                    eq(testUser.getFullName()),
                    eq(EmailTemplateName.ACTIVATE_ACCOUNT),
                    eq("http://localhost:4200/activate"),
                    eq("654321"),
                    eq("Account activation")
            );
        }

        @Test
        @DisplayName("Devrait échouer si l'utilisateur n'existe pas")
        void shouldFailWhenUserNotFound() {
            // Given
            String code = "123456";
            given(tokenRepository.findFirstByTokenAndTypeOrderByCreatedAtDesc(code, ACCOUNT_ACTIVATION))
                    .willReturn(Optional.of(validToken));
            given(userRepository.findById(testUser.getId())).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> authenticationService.activateAccount(code))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("authenticate() - Authentification")
    class AuthenticateTests {

        @Test
        @DisplayName("Devrait authentifier avec succès et retourner un token JWT")
        void shouldAuthenticateSuccessfully() {
            // Given
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("john.doe@test.com")
                    .password("password123")
                    .build();

            Authentication mockAuth = mock(Authentication.class);
            given(mockAuth.getPrincipal()).willReturn(testUser);
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(mockAuth);
            given(jwtService.generateToken(anyMap(), eq(testUser)))
                    .willReturn("jwt-token-123");

            // When
            AuthenticationResponse response = authenticationService.authenticate(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token-123");

            then(authenticationManager).should().authenticate(
                    argThat(auth -> auth.getPrincipal().equals("john.doe@test.com")
                            && auth.getCredentials().equals("password123"))
            );
        }

        @Test
        @DisplayName("Devrait propager l'exception si les credentials sont invalides")
        void shouldPropagateAuthenticationException() {
            // Given
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("john.doe@test.com")
                    .password("wrong-password")
                    .build();

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willThrow(new RuntimeException("Bad credentials"));

            // When / Then
            assertThatThrownBy(() -> authenticationService.authenticate(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Bad credentials");
        }
    }

    @Nested
    @DisplayName("forgotPassword() - Demande de réinitialisation")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Devrait envoyer un email de réinitialisation")
        void shouldSendPasswordResetEmail() throws MessagingException {
            // Given
            String email = "john.doe@test.com";
            given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
            given(tokenService.generateAndSaveActivationToken(testUser, FORGOT_PASSWORD))
                    .willReturn("123456");
            willDoNothing().given(emailService).sendEmail(anyString(), anyString(), any(), anyString(), anyString(), anyString());

            // When
            authenticationService.forgotPassword(email);

            // Then
            then(emailService).should().sendEmail(
                    eq(email),
                    eq(testUser.getFullName()),
                    eq(EmailTemplateName.FORGOT_PASSWORD),
                    eq("http://localhost:4200/reset"),
                    eq("123456"),
                    eq("Password reset request")
            );
        }

        @Test
        @DisplayName("Devrait échouer si l'utilisateur n'existe pas")
        void shouldFailWhenUserNotFound() throws MessagingException {
            // Given
            String email = "unknown@test.com";
            given(userRepository.findByEmail(email)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> authenticationService.forgotPassword(email))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("User not found");

            then(emailService).should(never()).sendEmail(anyString(), anyString(), any(), anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("resetPassword() - Réinitialisation du mot de passe")
    class ResetPasswordTests {

        @Test
        @DisplayName("Devrait réinitialiser le mot de passe avec succès")
        void shouldResetPasswordSuccessfully() {
            // Given
            var request = new ResetPasswordRequest();
            request.setToken("123456");
            request.setNewPassword("NewPassword123!");
            request.setConfirmPassword("NewPassword123!");

            Token resetToken = Token.builder()
                    .id(1)
                    .token("123456")
                    .type(FORGOT_PASSWORD)
                    .user(testUser)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .validatedAt(LocalDateTime.now())
                    .build();

            given(tokenRepository.findFirstByTokenAndTypeOrderByCreatedAtDesc("123456", FORGOT_PASSWORD))
                    .willReturn(Optional.of(resetToken));
            given(passwordEncoder.encode("NewPassword123!")).willReturn("new-encoded-password");
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            authenticationService.resetPassword(request);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("new-encoded-password");
        }

        @Test
        @DisplayName("Devrait échouer si les mots de passe ne correspondent pas")
        void shouldFailWhenPasswordsDoNotMatch() {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("123456");
            request.setNewPassword("password1");
            request.setConfirmPassword("password2");

            // When / Then
            assertThatThrownBy(() -> authenticationService.resetPassword(request))
                    .isInstanceOf(PasswordMismatchException.class)
                    .hasMessageContaining("Passwords do not match");

            then(userRepository).should(never()).save(any(User.class));
        }

        @Test
        @DisplayName("Devrait échouer si le token est invalide")
        void shouldFailWithInvalidToken() {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("invalid");
            request.setNewPassword("newPassword123");
            request.setConfirmPassword("newPassword123");

            given(tokenRepository.findFirstByTokenAndTypeOrderByCreatedAtDesc("invalid", FORGOT_PASSWORD))
                    .willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> authenticationService.resetPassword(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid reset token");
        }

        @Test
        @DisplayName("Devrait échouer si le token est expiré")
        void shouldFailWhenTokenExpired() {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("123456");
            request.setNewPassword("newPassword123");
            request.setConfirmPassword("newPassword123");

            Token expiredToken = Token.builder()
                    .id(1)
                    .token("123456")
                    .type(FORGOT_PASSWORD)
                    .user(testUser)
                    .createdAt(LocalDateTime.now().minusMinutes(30))
                    .expiresAt(LocalDateTime.now().minusMinutes(15))
                    .build();

            given(tokenRepository.findFirstByTokenAndTypeOrderByCreatedAtDesc("123456", FORGOT_PASSWORD))
                    .willReturn(Optional.of(expiredToken));

            // When / Then
            assertThatThrownBy(() -> authenticationService.resetPassword(request))
                    .isInstanceOf(ExpiredTokenException.class)
                    .hasMessageContaining("Token expired");
        }

        @Test
        @DisplayName("Devrait échouer si le code n'est pas vérifié")
        void shouldFailWhenCodeNotVerified() {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("123456");
            request.setNewPassword("newPassword123");
            request.setConfirmPassword("newPassword123");

            Token unverifiedToken = Token.builder()
                    .id(1)
                    .token("123456")
                    .type(FORGOT_PASSWORD)
                    .user(testUser)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .validatedAt(null)
                    .build();

            given(tokenRepository.findFirstByTokenAndTypeOrderByCreatedAtDesc("123456", FORGOT_PASSWORD))
                    .willReturn(Optional.of(unverifiedToken));

            // When / Then
            assertThatThrownBy(() -> authenticationService.resetPassword(request))
                    .isInstanceOf(CodeNotVerifiedException.class)
                    .hasMessageContaining("Code not verified");
        }
    }

    @Nested
    @DisplayName("verifyResetToken() - Vérification du token de réinitialisation")
    class VerifyResetTokenTests {

        @Test
        @DisplayName("Devrait vérifier le token avec succès")
        void shouldVerifyTokenSuccessfully() {
            // Given
            String otp = "123456";
            Token resetToken = Token.builder()
                    .id(1)
                    .token(otp)
                    .type(FORGOT_PASSWORD)
                    .user(testUser)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();

            given(tokenRepository.findFirstByTokenAndTypeOrderByCreatedAtDesc(otp, FORGOT_PASSWORD))
                    .willReturn(Optional.of(resetToken));
            given(tokenRepository.save(any(Token.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            authenticationService.verifyResetToken(otp);

            // Then
            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            then(tokenRepository).should().save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getValidatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Devrait échouer si le token est invalide")
        void shouldFailWithInvalidToken() {
            // Given
            String otp = "invalid";
            given(tokenRepository.findFirstByTokenAndTypeOrderByCreatedAtDesc(otp, FORGOT_PASSWORD))
                    .willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> authenticationService.verifyResetToken(otp))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid reset token");
        }

        @Test
        @DisplayName("Devrait échouer si le token est expiré")
        void shouldFailWhenTokenExpired() {
            // Given
            String otp = "123456";
            Token expiredToken = Token.builder()
                    .id(1)
                    .token(otp)
                    .type(FORGOT_PASSWORD)
                    .user(testUser)
                    .createdAt(LocalDateTime.now().minusMinutes(30))
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .build();

            given(tokenRepository.findFirstByTokenAndTypeOrderByCreatedAtDesc(otp, FORGOT_PASSWORD))
                    .willReturn(Optional.of(expiredToken));

            // When / Then
            assertThatThrownBy(() -> authenticationService.verifyResetToken(otp))
                    .isInstanceOf(ExpiredTokenException.class)
                    .hasMessageContaining("Token expired");
        }
    }

    @Nested
    @DisplayName("createUser() - Création d'utilisateur par admin")
    class CreateUserTests {

        @Test
        @DisplayName("Devrait créer un utilisateur et envoyer un email de configuration")
        void shouldCreateUserSuccessfully() throws MessagingException {
            // Given
            UserRequest request = new UserRequest(
                    "Alice",
                    "Wonder",
                    null,
                    "alice.wonder@test.com"
            );

            given(roleRepository.findByName("USER")).willReturn(Optional.of(userRole));
            given(passwordEncoder.encode("")).willReturn("empty-password");
            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(99);
                return user;
            });
            given(tokenService.generateAndSaveActivationToken(any(User.class), eq(SET_PASSWORD)))
                    .willReturn("789012");
            willDoNothing().given(emailService).sendEmail(anyString(), anyString(), any(), anyString(), anyString(), anyString());

            // When
            Integer userId = authenticationService.createUser(request);

            // Then
            assertThat(userId).isEqualTo(99);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(userCaptor.capture());
            
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getFirstName()).isEqualTo("Alice");
            assertThat(savedUser.getLastName()).isEqualTo("Wonder");
            assertThat(savedUser.getEmail()).isEqualTo("alice.wonder@test.com");
            assertThat(savedUser.isEnabled()).isTrue();
            assertThat(savedUser.getRoles()).contains(userRole);

            then(emailService).should().sendEmail(
                    eq("alice.wonder@test.com"),
                    eq("Alice Wonder"),
                    eq(EmailTemplateName.SET_PASSWORD),
                    eq("http://localhost:4200/set-password"),
                    eq("789012"),
                    eq("Set your password")
            );
        }

        @Test
        @DisplayName("Devrait échouer si le rôle USER n'existe pas")
        void shouldFailWhenUserRoleNotFound() {
            // Given
            UserRequest request = new UserRequest(
                    "Alice",
                    "Wonder",
                    null,
                    "alice.wonder@test.com"
            );

            given(roleRepository.findByName("USER")).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> authenticationService.createUser(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("User Role Not Found");

            then(userRepository).should(never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("sendValidationEmail() - Envoi d'email de validation")
    class SendValidationEmailTests {

        @Test
        @DisplayName("Devrait envoyer un email de validation avec succès")
        void shouldSendValidationEmailSuccessfully() throws MessagingException {
            // Given
            given(tokenService.generateAndSaveActivationToken(testUser, ACCOUNT_ACTIVATION))
                    .willReturn("123456");
            willDoNothing().given(emailService).sendEmail(anyString(), anyString(), any(), anyString(), anyString(), anyString());

            // When
            authenticationService.sendValidationEmail(testUser);

            // Then
            then(emailService).should().sendEmail(
                    eq(testUser.getEmail()),
                    eq(testUser.getFullName()),
                    eq(EmailTemplateName.ACTIVATE_ACCOUNT),
                    eq("http://localhost:4200/activate"),
                    eq("123456"),
                    eq("Account activation")
            );
            then(tokenService).should(never()).deleteToken(anyString());
        }

        @Test
        @DisplayName("Devrait nettoyer le token si l'envoi d'email échoue")
        void shouldCleanupTokenWhenEmailFails() throws MessagingException {
            // Given
            given(tokenService.generateAndSaveActivationToken(testUser, ACCOUNT_ACTIVATION))
                    .willReturn("123456");
            willThrow(new MessagingException("Email server down"))
                    .given(emailService).sendEmail(anyString(), anyString(), any(), anyString(), anyString(), anyString());

            // When / Then
            assertThatThrownBy(() -> authenticationService.sendValidationEmail(testUser))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("Email server down");

            then(tokenService).should().deleteToken("123456");
        }
    }
}
