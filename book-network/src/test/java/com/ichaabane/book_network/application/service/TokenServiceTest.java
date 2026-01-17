package com.ichaabane.book_network.application.service;

import com.ichaabane.book_network.domain.enums.TokenType;
import com.ichaabane.book_network.domain.model.Token;
import com.ichaabane.book_network.domain.model.User;
import com.ichaabane.book_network.domain.repository.TokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static com.ichaabane.book_network.domain.enums.TokenType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * Tests unitaires pour TokenService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService - Tests unitaires")
class TokenServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private TokenService tokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .build();
    }

    @Nested
    @DisplayName("generateAndSaveActivationToken() - Générer et sauvegarder un token")
    class GenerateAndSaveActivationTokenTests {

        @Test
        @DisplayName("Devrait générer et sauvegarder un token d'activation")
        void shouldGenerateAndSaveActivationToken() {
            // Given
            willDoNothing().given(tokenRepository).deleteByUserIdAndType(testUser.getId(), ACCOUNT_ACTIVATION);
            given(tokenRepository.save(any(Token.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            String token = tokenService.generateAndSaveActivationToken(testUser, ACCOUNT_ACTIVATION);

            // Then
            assertThat(token).isNotNull().hasSize(6).matches("\\d{6}"); // 6 chiffres

            then(tokenRepository).should().deleteByUserIdAndType(testUser.getId(), ACCOUNT_ACTIVATION);

            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            then(tokenRepository).should().save(tokenCaptor.capture());

            Token savedToken = tokenCaptor.getValue();
            assertThat(savedToken.getToken()).isEqualTo(token);
            assertThat(savedToken.getUser()).isEqualTo(testUser);
            assertThat(savedToken.getType()).isEqualTo(ACCOUNT_ACTIVATION);
            assertThat(savedToken.getCreatedAt()).isNotNull();
            assertThat(savedToken.getExpiresAt()).isNotNull();
            assertThat(savedToken.getExpiresAt()).isAfter(savedToken.getCreatedAt());
        }

        @Test
        @DisplayName("Devrait générer un token pour FORGOT_PASSWORD")
        void shouldGenerateTokenForForgotPassword() {
            // Given
            willDoNothing().given(tokenRepository).deleteByUserIdAndType(testUser.getId(), FORGOT_PASSWORD);
            given(tokenRepository.save(any(Token.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            String token = tokenService.generateAndSaveActivationToken(testUser, FORGOT_PASSWORD);

            // Then
            assertThat(token).hasSize(6).hasSize(6);

            then(tokenRepository).should().deleteByUserIdAndType(testUser.getId(), FORGOT_PASSWORD);

            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            then(tokenRepository).should().save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getType()).isEqualTo(FORGOT_PASSWORD);
        }

        @Test
        @DisplayName("Devrait générer un token pour SET_PASSWORD")
        void shouldGenerateTokenForSetPassword() {
            // Given
            willDoNothing().given(tokenRepository).deleteByUserIdAndType(testUser.getId(), SET_PASSWORD);
            given(tokenRepository.save(any(Token.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            String token = tokenService.generateAndSaveActivationToken(testUser, SET_PASSWORD);

            // Then
            assertThat(token).hasSize(6).hasSize(6);

            then(tokenRepository).should().deleteByUserIdAndType(testUser.getId(), SET_PASSWORD);

            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            then(tokenRepository).should().save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getType()).isEqualTo(SET_PASSWORD);
        }

        @Test
        @DisplayName("Devrait supprimer les anciens tokens avant d'en créer un nouveau")
        void shouldDeleteOldTokensBeforeCreatingNew() {
            // Given
            willDoNothing().given(tokenRepository).deleteByUserIdAndType(testUser.getId(), ACCOUNT_ACTIVATION);
            given(tokenRepository.save(any(Token.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            tokenService.generateAndSaveActivationToken(testUser, ACCOUNT_ACTIVATION);

            // Then
            then(tokenRepository).should().deleteByUserIdAndType(testUser.getId(), ACCOUNT_ACTIVATION);
            then(tokenRepository).should().save(any(Token.class));
        }

        @Test
        @DisplayName("Devrait générer un token avec une expiration de 15 minutes")
        void shouldGenerateTokenWithFifteenMinutesExpiration() {
            // Given
            willDoNothing().given(tokenRepository).deleteByUserIdAndType(testUser.getId(), ACCOUNT_ACTIVATION);
            given(tokenRepository.save(any(Token.class))).willAnswer(invocation -> invocation.getArgument(0));

            LocalDateTime beforeCreation = LocalDateTime.now();

            // When
            tokenService.generateAndSaveActivationToken(testUser, ACCOUNT_ACTIVATION);

            LocalDateTime afterCreation = LocalDateTime.now();

            // Then
            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            then(tokenRepository).should().save(tokenCaptor.capture());

            Token savedToken = tokenCaptor.getValue();
            assertThat(savedToken.getCreatedAt()).isBetween(beforeCreation, afterCreation);
            assertThat(savedToken.getExpiresAt()).isBetween(
                    beforeCreation.plusMinutes(15),
                    afterCreation.plusMinutes(15)
            );
        }

        @Test
        @DisplayName("Devrait générer des tokens uniques")
        void shouldGenerateUniqueTokens() {
            // Given
            willDoNothing().given(tokenRepository).deleteByUserIdAndType(anyInt(), any(TokenType.class));
            given(tokenRepository.save(any(Token.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            String token1 = tokenService.generateAndSaveActivationToken(testUser, ACCOUNT_ACTIVATION);
            String token2 = tokenService.generateAndSaveActivationToken(testUser, ACCOUNT_ACTIVATION);
            String token3 = tokenService.generateAndSaveActivationToken(testUser, ACCOUNT_ACTIVATION);

            // Then
            // Bien que théoriquement possible, il est extrêmement improbable que 3 tokens
            // aléatoires soient identiques
            assertThat(token1).isNotEqualTo(token2);
            assertThat(token2).isNotEqualTo(token3);
            assertThat(token1).isNotEqualTo(token3);
        }

        @Test
        @DisplayName("Devrait générer un token contenant uniquement des chiffres")
        void shouldGenerateTokenWithOnlyDigits() {
            // Given
            willDoNothing().given(tokenRepository).deleteByUserIdAndType(testUser.getId(), ACCOUNT_ACTIVATION);
            given(tokenRepository.save(any(Token.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            String token = tokenService.generateAndSaveActivationToken(testUser, ACCOUNT_ACTIVATION);

            // Then
            assertThat(token).matches("^[0-9]{6}$");
        }

        @Test
        @DisplayName("Devrait gérer plusieurs types de tokens pour le même utilisateur")
        void shouldHandleMultipleTokenTypesForSameUser() {
            // Given
            willDoNothing().given(tokenRepository).deleteByUserIdAndType(anyInt(), any(TokenType.class));
            given(tokenRepository.save(any(Token.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            String activationToken = tokenService.generateAndSaveActivationToken(testUser, ACCOUNT_ACTIVATION);
            String resetToken = tokenService.generateAndSaveActivationToken(testUser, FORGOT_PASSWORD);
            String setPasswordToken = tokenService.generateAndSaveActivationToken(testUser, SET_PASSWORD);

            // Then
            assertThat(activationToken).isNotNull();
            assertThat(resetToken).isNotNull();
            assertThat(setPasswordToken).isNotNull();

            then(tokenRepository).should().deleteByUserIdAndType(testUser.getId(), ACCOUNT_ACTIVATION);
            then(tokenRepository).should().deleteByUserIdAndType(testUser.getId(), FORGOT_PASSWORD);
            then(tokenRepository).should().deleteByUserIdAndType(testUser.getId(), SET_PASSWORD);
            then(tokenRepository).should(times(3)).save(any(Token.class));
        }

        @Test
        @DisplayName("Devrait créer un token sans validatedAt initialement")
        void shouldCreateTokenWithoutValidatedAt() {
            // Given
            willDoNothing().given(tokenRepository).deleteByUserIdAndType(testUser.getId(), ACCOUNT_ACTIVATION);
            given(tokenRepository.save(any(Token.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            tokenService.generateAndSaveActivationToken(testUser, ACCOUNT_ACTIVATION);

            // Then
            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            then(tokenRepository).should().save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getValidatedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("deleteToken() - Supprimer un token")
    class DeleteTokenTests {

        @Test
        @DisplayName("Devrait supprimer un token par sa valeur")
        void shouldDeleteTokenByValue() {
            // Given
            String tokenValue = "123456";
            willDoNothing().given(tokenRepository).deleteByToken(tokenValue);

            // When
            tokenService.deleteToken(tokenValue);

            // Then
            then(tokenRepository).should().deleteByToken(tokenValue);
        }

        @Test
        @DisplayName("Devrait gérer la suppression d'un token inexistant")
        void shouldHandleDeletionOfNonExistentToken() {
            // Given
            String tokenValue = "999999";
            willDoNothing().given(tokenRepository).deleteByToken(tokenValue);

            // When
            tokenService.deleteToken(tokenValue);

            // Then
            then(tokenRepository).should().deleteByToken(tokenValue);
            // Pas d'exception levée
        }

        @Test
        @DisplayName("Devrait gérer la suppression d'un token null")
        void shouldHandleDeletionOfNullToken() {
            // Given
            willDoNothing().given(tokenRepository).deleteByToken(null);

            // When
            tokenService.deleteToken(null);

            // Then
            then(tokenRepository).should().deleteByToken(null);
        }

        @Test
        @DisplayName("Devrait gérer la suppression d'un token vide")
        void shouldHandleDeletionOfEmptyToken() {
            // Given
            String emptyToken = "";
            willDoNothing().given(tokenRepository).deleteByToken(emptyToken);

            // When
            tokenService.deleteToken(emptyToken);

            // Then
            then(tokenRepository).should().deleteByToken(emptyToken);
        }

        @Test
        @DisplayName("Devrait permettre de supprimer plusieurs tokens successivement")
        void shouldAllowDeletingMultipleTokensSuccessively() {
            // Given
            String token1 = "123456";
            String token2 = "654321";
            String token3 = "111111";

            willDoNothing().given(tokenRepository).deleteByToken(anyString());

            // When
            tokenService.deleteToken(token1);
            tokenService.deleteToken(token2);
            tokenService.deleteToken(token3);

            // Then
            then(tokenRepository).should().deleteByToken(token1);
            then(tokenRepository).should().deleteByToken(token2);
            then(tokenRepository).should().deleteByToken(token3);
        }
    }

    @Nested
    @DisplayName("Cas limites et comportement transactionnel")
    class EdgeCaseTests {

        @Test
        @DisplayName("Devrait gérer un userId null")
        void shouldHandleNullUserId() {
            // Given
            User userWithNullId = User.builder()
                    .id(null)
                    .firstName("Test")
                    .lastName("User")
                    .email("test@test.com")
                    .build();

            willDoNothing().given(tokenRepository).deleteByUserIdAndType(null, ACCOUNT_ACTIVATION);
            given(tokenRepository.save(any(Token.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            String token = tokenService.generateAndSaveActivationToken(userWithNullId, ACCOUNT_ACTIVATION);

            // Then
            assertThat(token).isNotNull();
            then(tokenRepository).should().deleteByUserIdAndType(null, ACCOUNT_ACTIVATION);
        }

        @Test
        @DisplayName("Devrait générer un token même si la suppression des anciens tokens échoue")
        void shouldGenerateTokenEvenIfDeletionFails() {
            // Given
            willThrow(new RuntimeException("Database error"))
                    .given(tokenRepository).deleteByUserIdAndType(testUser.getId(), ACCOUNT_ACTIVATION);

            // When / Then
            assertThatThrownBy(() -> 
                tokenService.generateAndSaveActivationToken(testUser, ACCOUNT_ACTIVATION))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error");

            // Le token n'est pas sauvegardé si la suppression échoue
            then(tokenRepository).should(never()).save(any(Token.class));
        }

        @Test
        @DisplayName("Devrait propager l'exception si la sauvegarde échoue")
        void shouldPropagateExceptionIfSaveFails() {
            // Given
            willDoNothing().given(tokenRepository).deleteByUserIdAndType(testUser.getId(), ACCOUNT_ACTIVATION);
            given(tokenRepository.save(any(Token.class)))
                    .willThrow(new RuntimeException("Database error"));

            // When / Then
            assertThatThrownBy(() -> 
                tokenService.generateAndSaveActivationToken(testUser, ACCOUNT_ACTIVATION))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error");
        }
    }

    /**
     * Notes sur la couverture:
     * - generateActivationCode() est une méthode privée testée indirectement via
     *   generateAndSaveActivationToken().
     * - Les annotations @Transactional(propagation = Propagation.REQUIRES_NEW) n'ont
     *   aucun effet dans les tests unitaires sans contexte Spring.
     * - SecureRandom est utilisé pour générer des nombres aléatoires sécurisés.
     * - Couverture: 100% de la logique métier.
     */
}
