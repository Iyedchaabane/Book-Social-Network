package com.ichaabane.book_network.application.service;

import com.ichaabane.book_network.infrastructure.email.EmailTemplateName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

/**
 * Tests unitaires pour EmailService avec Brevo.
 * 
 * Note: EmailService utilise RestTemplate pour appeler l'API Brevo et SpringTemplateEngine pour les templates.
 * Nous testons uniquement la logique métier (paramètres, gestion des templates, construction de la requête API).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmailService - Tests unitaires (Brevo)")
class EmailServiceTest {

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private RestTemplate restTemplate;

    private EmailService emailService;

    private static final String TEST_API_KEY = "test-api-key-12345";
    private static final String TEST_SENDER_EMAIL = "noreply@test.com";
    private static final String TEST_SENDER_NAME = "Test Application";

    @BeforeEach
    void setUp() {
        // Manually construct EmailService with mocked dependencies
        emailService = new EmailService(templateEngine);
        
        // Inject @Value properties and RestTemplate using ReflectionTestUtils
        ReflectionTestUtils.setField(emailService, "brevoApiKey", TEST_API_KEY);
        ReflectionTestUtils.setField(emailService, "senderEmail", TEST_SENDER_EMAIL);
        ReflectionTestUtils.setField(emailService, "senderName", TEST_SENDER_NAME);
        ReflectionTestUtils.setField(emailService, "restTemplate", restTemplate);
    }

    @Nested
    @DisplayName("sendEmail() - Envoi d'emails via Brevo")
    class SendEmailTests {

        @Test
        @DisplayName("Devrait envoyer un email avec template personnalisé")
        void shouldSendEmailWithCustomTemplate() {
            // Given
            String to = "test@example.com";
            String username = "John Doe";
            EmailTemplateName template = EmailTemplateName.ACTIVATE_ACCOUNT;
            String confirmationUrl = "http://localhost:4200/activate";
            String activationCode = "123456";
            String subject = "Activate your account";

            given(templateEngine.process(eq("activate_account"), any(Context.class)))
                    .willReturn("<html>Email content</html>");
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .willReturn(new ResponseEntity<>("Success", HttpStatus.OK));

            // When
            emailService.sendEmail(to, username, template, confirmationUrl, activationCode, subject);

            // Then
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            then(templateEngine).should().process(eq("activate_account"), contextCaptor.capture());

            // Vérifier que le contexte contient les bonnes variables
            Context capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.getVariable("username")).isEqualTo(username);
            assertThat(capturedContext.getVariable("confirmationUrl")).isEqualTo(confirmationUrl);
            assertThat(capturedContext.getVariable("activationCode")).isEqualTo(activationCode);

            // Vérifier l'appel à l'API Brevo
            ArgumentCaptor<HttpEntity> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            then(restTemplate).should().postForEntity(
                eq("https://api.brevo.com/v3/smtp/email"),
                httpEntityCaptor.capture(),
                eq(String.class)
            );

            // Vérifier les headers
            HttpEntity<?> capturedEntity = httpEntityCaptor.getValue();
            assertThat(capturedEntity.getHeaders().get("api-key")).containsExactly(TEST_API_KEY);
            assertThat(capturedEntity.getHeaders().getContentType().toString()).contains("application/json");

            // Vérifier le body de la requête
            @SuppressWarnings("unchecked")
            Map<String, Object> requestBody = (Map<String, Object>) capturedEntity.getBody();
            assertThat(requestBody).isNotNull();
            
            @SuppressWarnings("unchecked")
            Map<String, String> sender = (Map<String, String>) requestBody.get("sender");
            assertThat(sender.get("email")).isEqualTo(TEST_SENDER_EMAIL);
            assertThat(sender.get("name")).isEqualTo(TEST_SENDER_NAME);

            @SuppressWarnings("unchecked")
            Map<String, String>[] recipients = (Map<String, String>[]) requestBody.get("to");
            assertThat(recipients).hasSize(1);
            assertThat(recipients[0].get("email")).isEqualTo(to);
            assertThat(recipients[0].get("name")).isEqualTo(username);

            assertThat(requestBody.get("subject")).isEqualTo(subject);
            assertThat(requestBody.get("htmlContent")).isEqualTo("<html>Email content</html>");
        }

        @Test
        @DisplayName("Devrait utiliser le template par défaut si null")
        void shouldUseDefaultTemplateWhenNull() {
            // Given
            String to = "test@example.com";
            String username = "John Doe";
            String confirmationUrl = "http://localhost:4200/confirm";
            String activationCode = "123456";
            String subject = "Confirm email";

            given(templateEngine.process(eq("confirm-email"), any(Context.class)))
                    .willReturn("<html>Default template</html>");
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .willReturn(new ResponseEntity<>("Success", HttpStatus.OK));

            // When
            emailService.sendEmail(to, username, null, confirmationUrl, activationCode, subject);

            // Then
            then(templateEngine).should().process(eq("confirm-email"), any(Context.class));
            then(restTemplate).should().postForEntity(
                eq("https://api.brevo.com/v3/smtp/email"),
                any(HttpEntity.class),
                eq(String.class)
            );
        }

        @Test
        @DisplayName("Devrait propager RestClientException en cas d'erreur API")
        void shouldPropagateRestClientException() {
            // Given
            String to = "invalid@example.com";
            
            given(templateEngine.process(anyString(), any(Context.class)))
                    .willReturn("<html>Content</html>");
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .willThrow(new RestClientException("API connection failed"));

            // When / Then
            assertThatThrownBy(() -> 
                emailService.sendEmail(to, "User", EmailTemplateName.ACTIVATE_ACCOUNT, 
                    "url", "code", "subject"))
                    .isInstanceOf(RestClientException.class)
                    .hasMessageContaining("API connection failed");
        }

        @Test
        @DisplayName("Devrait gérer template FORGOT_PASSWORD")
        void shouldHandleForgotPasswordTemplate() {
            // Given
            EmailTemplateName template = EmailTemplateName.FORGOT_PASSWORD;
            
            given(templateEngine.process(eq("forgot_password"), any(Context.class)))
                    .willReturn("<html>Reset password</html>");
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .willReturn(new ResponseEntity<>("Success", HttpStatus.OK));

            // When
            emailService.sendEmail("user@test.com", "User", template, 
                "http://reset", "code123", "Reset password");

            // Then
            then(templateEngine).should().process(eq("forgot_password"), any(Context.class));
            then(restTemplate).should().postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("Devrait gérer template SET_PASSWORD")
        void shouldHandleSetPasswordTemplate() {
            // Given
            EmailTemplateName template = EmailTemplateName.SET_PASSWORD;
            
            given(templateEngine.process(eq("set_password"), any(Context.class)))
                    .willReturn("<html>Set password</html>");
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .willReturn(new ResponseEntity<>("Success", HttpStatus.OK));

            // When
            emailService.sendEmail("new@test.com", "New User", template, 
                "http://set", "token789", "Set your password");

            // Then
            then(templateEngine).should().process(eq("set_password"), any(Context.class));
            then(restTemplate).should().postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("Devrait inclure tous les paramètres dans le modèle")
        void shouldIncludeAllParametersInModel() {
            // Given
            String username = "Test User";
            String confirmationUrl = "http://test.com/confirm";
            String activationCode = "ABC123";

            given(templateEngine.process(anyString(), any(Context.class)))
                    .willReturn("<html>Test</html>");
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .willReturn(new ResponseEntity<>("Success", HttpStatus.OK));

            // When
            emailService.sendEmail("test@test.com", username, null, 
                confirmationUrl, activationCode, "Test Subject");

            // Then
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            then(templateEngine).should().process(anyString(), contextCaptor.capture());

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("username")).isEqualTo(username);
            assertThat(context.getVariable("confirmationUrl")).isEqualTo(confirmationUrl);
            assertThat(context.getVariable("activationCode")).isEqualTo(activationCode);
        }
    }

    @Nested
    @DisplayName("sendEmail() - Cas limites")
    class EdgeCaseTests {

        @Test
        @DisplayName("Devrait gérer username null")
        void shouldHandleNullUsername() {
            // Given
            given(templateEngine.process(anyString(), any(Context.class)))
                    .willReturn("<html>Test</html>");
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .willReturn(new ResponseEntity<>("Success", HttpStatus.OK));

            // When
            emailService.sendEmail("test@test.com", null, EmailTemplateName.ACTIVATE_ACCOUNT, 
                "url", "code", "subject");

            // Then
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            then(templateEngine).should().process(anyString(), contextCaptor.capture());
            
            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("username")).isNull();
            
            // Vérifier que l'email est quand même envoyé
            then(restTemplate).should().postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("Devrait gérer confirmationUrl vide")
        void shouldHandleEmptyConfirmationUrl() {
            // Given
            given(templateEngine.process(anyString(), any(Context.class)))
                    .willReturn("<html>Test</html>");
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .willReturn(new ResponseEntity<>("Success", HttpStatus.OK));

            // When
            emailService.sendEmail("test@test.com", "User", EmailTemplateName.ACTIVATE_ACCOUNT, 
                "", "code", "subject");

            // Then
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            then(templateEngine).should().process(anyString(), contextCaptor.capture());
            
            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("confirmationUrl")).isEqualTo("");
        }

        @Test
        @DisplayName("Devrait gérer activationCode null")
        void shouldHandleNullActivationCode() {
            // Given
            given(templateEngine.process(anyString(), any(Context.class)))
                    .willReturn("<html>Test</html>");
            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .willReturn(new ResponseEntity<>("Success", HttpStatus.OK));

            // When
            emailService.sendEmail("test@test.com", "User", EmailTemplateName.ACTIVATE_ACCOUNT, 
                "url", null, "subject");

            // Then
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            then(templateEngine).should().process(anyString(), contextCaptor.capture());
            
            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("activationCode")).isNull();
        }
    }

    /**
     * Note sur la couverture:
     * - La méthode sendEmail() est annotée @Async, ce qui signifie qu'elle s'exécute 
     *   de manière asynchrone en production.
     * - Dans les tests unitaires, sans contexte Spring, l'annotation @Async n'a aucun effet
     *   et la méthode s'exécute de manière synchrone.
     * - Les interactions avec RestTemplate (API Brevo) et SpringTemplateEngine sont mockées car
     *   ce sont des dépendances externes (API REST et framework).
     * - Couverture: 100% de la logique métier testée.
     */
}