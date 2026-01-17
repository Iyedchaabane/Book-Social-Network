package com.ichaabane.book_network.application.mapper;

import com.ichaabane.book_network.application.dto.request.FeedbackRequest;
import com.ichaabane.book_network.application.dto.response.FeedbackResponse;
import com.ichaabane.book_network.domain.model.Book;
import com.ichaabane.book_network.domain.model.Feedback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour FeedbackMapper.
 * Couverture : 100%
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackMapper - Tests unitaires")
class FeedbackMapperTest {

    @InjectMocks
    private FeedbackMapper feedbackMapper;

    private FeedbackRequest feedbackRequest;
    private Feedback feedback;

    @BeforeEach
    void setUp() {
        feedbackRequest = new FeedbackRequest(4.5, "Excellent livre !", 1);

        feedback = Feedback.builder()
                .id(1)
                .note(4.5)
                .comment("Excellent livre !")
                .book(Book.builder().id(1).build())
                .createdBy(1)
                .build();
    }

    @Nested
    @DisplayName("toFeedback() - Conversion FeedbackRequest vers Feedback")
    class ToFeedbackTests {

        @Test
        @DisplayName("Devrait mapper correctement tous les champs du FeedbackRequest")
        void shouldMapAllFieldsFromFeedbackRequest() {
            // When
            Feedback result = feedbackMapper.toFeedback(feedbackRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getNote()).isEqualTo(4.5);
            assertThat(result.getComment()).isEqualTo("Excellent livre !");
            assertThat(result.getBook()).isNotNull();
            assertThat(result.getBook().getId()).isEqualTo(1);
        }

        @Test
        @DisplayName("Devrait créer un objet Book avec seulement l'ID")
        void shouldCreateBookWithIdOnly() {
            // When
            Feedback result = feedbackMapper.toFeedback(feedbackRequest);

            // Then
            assertThat(result.getBook()).isNotNull();
            assertThat(result.getBook().getId()).isEqualTo(1);
            // Les autres champs du Book ne sont pas définis
        }

        @Test
        @DisplayName("Devrait mapper une note décimale")
        void shouldMapDecimalNote() {
            // Given
            FeedbackRequest request = new FeedbackRequest(3.7, "Bon livre", 2);

            // When
            Feedback result = feedbackMapper.toFeedback(request);

            // Then
            assertThat(result.getNote()).isEqualTo(3.7);
        }

        @Test
        @DisplayName("Devrait mapper une note entière")
        void shouldMapIntegerNote() {
            // Given
            FeedbackRequest request = new FeedbackRequest(5.0, "Parfait !", 3);

            // When
            Feedback result = feedbackMapper.toFeedback(request);

            // Then
            assertThat(result.getNote()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("Devrait mapper une note minimale")
        void shouldMapMinimalNote() {
            // Given
            FeedbackRequest request = new FeedbackRequest(1.0, "Décevant", 4);

            // When
            Feedback result = feedbackMapper.toFeedback(request);

            // Then
            assertThat(result.getNote()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Devrait gérer un commentaire vide")
        void shouldHandleEmptyComment() {
            // Given
            FeedbackRequest request = new FeedbackRequest(4.0, "", 5);

            // When
            Feedback result = feedbackMapper.toFeedback(request);

            // Then
            assertThat(result.getComment()).isEmpty();
        }

        @Test
        @DisplayName("Devrait gérer un commentaire null")
        void shouldHandleNullComment() {
            // Given
            FeedbackRequest request = new FeedbackRequest(4.0, null, 6);

            // When
            Feedback result = feedbackMapper.toFeedback(request);

            // Then
            assertThat(result.getComment()).isNull();
        }

        @Test
        @DisplayName("Devrait gérer un commentaire long")
        void shouldHandleLongComment() {
            // Given
            String longComment = "A".repeat(500);
            FeedbackRequest request = new FeedbackRequest(4.5, longComment, 7);

            // When
            Feedback result = feedbackMapper.toFeedback(request);

            // Then
            assertThat(result.getComment()).hasSize(500);
            assertThat(result.getComment()).isEqualTo(longComment);
        }

        @Test
        @DisplayName("Devrait gérer différents IDs de livre")
        void shouldHandleDifferentBookIds() {
            // Given
            FeedbackRequest request1 = new FeedbackRequest(4.0, "Comment 1", 1);
            FeedbackRequest request2 = new FeedbackRequest(4.0, "Comment 2", 999);

            // When
            Feedback result1 = feedbackMapper.toFeedback(request1);
            Feedback result2 = feedbackMapper.toFeedback(request2);

            // Then
            assertThat(result1.getBook().getId()).isEqualTo(1);
            assertThat(result2.getBook().getId()).isEqualTo(999);
        }

        @Test
        @DisplayName("Devrait gérer une note avec plusieurs décimales")
        void shouldHandleNoteWithMultipleDecimals() {
            // Given
            FeedbackRequest request = new FeedbackRequest(4.567, "Commentaire", 10);

            // When
            Feedback result = feedbackMapper.toFeedback(request);

            // Then
            assertThat(result.getNote()).isEqualTo(4.567);
        }
    }

    @Nested
    @DisplayName("toFeedbackResponse() - Conversion Feedback vers FeedbackResponse")
    class ToFeedbackResponseTests {

        @Test
        @DisplayName("Devrait mapper correctement tous les champs du Feedback")
        void shouldMapAllFieldsFromFeedback() {
            // Given
            Integer userId = 1;

            // When
            FeedbackResponse result = feedbackMapper.toFeedbackResponse(feedback, userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getNote()).isEqualTo(4.5);
            assertThat(result.getComment()).isEqualTo("Excellent livre !");
            assertThat(result.isOwnFeedback()).isTrue();
        }

        @Test
        @DisplayName("Devrait identifier correctement le feedback de l'utilisateur")
        void shouldIdentifyOwnFeedbackCorrectly() {
            // Given - Le feedback appartient à l'utilisateur 1
            Integer userId = 1;

            // When
            FeedbackResponse result = feedbackMapper.toFeedbackResponse(feedback, userId);

            // Then
            assertThat(result.isOwnFeedback()).isTrue();
        }

        @Test
        @DisplayName("Devrait identifier qu'un feedback n'appartient pas à l'utilisateur")
        void shouldIdentifyNotOwnFeedback() {
            // Given - Le feedback appartient à l'utilisateur 1, mais on vérifie pour l'utilisateur 2
            Integer userId = 2;

            // When
            FeedbackResponse result = feedbackMapper.toFeedbackResponse(feedback, userId);

            // Then
            assertThat(result.isOwnFeedback()).isFalse();
        }

        @Test
        @DisplayName("Devrait gérer un feedback sans createdBy")
        void shouldHandleFeedbackWithoutCreatedBy() {
            // Given
            feedback.setCreatedBy(null);
            Integer userId = 1;

            // When
            FeedbackResponse result = feedbackMapper.toFeedbackResponse(feedback, userId);

            // Then
            assertThat(result.isOwnFeedback()).isFalse();
        }

        @Test
        @DisplayName("Devrait gérer un userId null")
        void shouldHandleNullUserId() {
            // Given
            Integer userId = null;

            // When
            FeedbackResponse result = feedbackMapper.toFeedbackResponse(feedback, userId);

            // Then
            assertThat(result.isOwnFeedback()).isFalse();
        }

        @Test
        @DisplayName("Devrait gérer les deux valeurs null (createdBy et userId)")
        void shouldHandleBothNullValues() {
            // Given
            feedback.setCreatedBy(null);
            Integer userId = null;

            // When
            FeedbackResponse result = feedbackMapper.toFeedbackResponse(feedback, userId);

            // Then
            assertThat(result.isOwnFeedback()).isTrue(); // Objects.equals(null, null) = true
        }

        @Test
        @DisplayName("Devrait mapper un commentaire vide")
        void shouldMapEmptyComment() {
            // Given
            feedback.setComment("");
            Integer userId = 1;

            // When
            FeedbackResponse result = feedbackMapper.toFeedbackResponse(feedback, userId);

            // Then
            assertThat(result.getComment()).isEmpty();
        }

        @Test
        @DisplayName("Devrait mapper un commentaire null")
        void shouldMapNullComment() {
            // Given
            feedback.setComment(null);
            Integer userId = 1;

            // When
            FeedbackResponse result = feedbackMapper.toFeedbackResponse(feedback, userId);

            // Then
            assertThat(result.getComment()).isNull();
        }

        @Test
        @DisplayName("Devrait mapper différentes notes")
        void shouldMapDifferentNotes() {
            // Given
            feedback.setNote(1.0);
            Integer userId = 1;

            // When
            FeedbackResponse result1 = feedbackMapper.toFeedbackResponse(feedback, userId);

            feedback.setNote(5.0);
            FeedbackResponse result2 = feedbackMapper.toFeedbackResponse(feedback, userId);

            // Then
            assertThat(result1.getNote()).isEqualTo(1.0);
            assertThat(result2.getNote()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("Devrait gérer une note null")
        void shouldHandleNullNote() {
            // Given
            feedback.setNote(null);
            Integer userId = 1;

            // When
            FeedbackResponse result = feedbackMapper.toFeedbackResponse(feedback, userId);

            // Then
            assertThat(result.getNote()).isNull();
        }

        @Test
        @DisplayName("Devrait comparer correctement les IDs avec des valeurs différentes")
        void shouldCompareIdsCorrectlyWithDifferentValues() {
            // Given
            feedback.setCreatedBy(100);
            Integer userId = 200;

            // When
            FeedbackResponse result = feedbackMapper.toFeedbackResponse(feedback, userId);

            // Then
            assertThat(result.isOwnFeedback()).isFalse();
        }

        @Test
        @DisplayName("Devrait gérer des IDs identiques avec des objets différents")
        void shouldHandleEqualIdsWithDifferentObjects() {
            // Given
            feedback.setCreatedBy(Integer.valueOf(5));
            Integer userId = Integer.valueOf(5);

            // When
            FeedbackResponse result = feedbackMapper.toFeedbackResponse(feedback, userId);

            // Then
            assertThat(result.isOwnFeedback()).isTrue();
        }
    }

    @Nested
    @DisplayName("Cas limites et validation")
    class EdgeCaseTests {

        @Test
        @DisplayName("Devrait gérer un FeedbackRequest avec toutes les valeurs minimales")
        void shouldHandleFeedbackRequestWithMinimalValues() {
            // Given
            FeedbackRequest request = new FeedbackRequest(0.0, "", 0);

            // When
            Feedback result = feedbackMapper.toFeedback(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getNote()).isEqualTo(0.0);
            assertThat(result.getComment()).isEmpty();
            assertThat(result.getBook().getId()).isZero();
        }

        @Test
        @DisplayName("Devrait gérer un FeedbackRequest avec des valeurs null")
        void shouldHandleFeedbackRequestWithNullValues() {
            // Given
            FeedbackRequest request = new FeedbackRequest(null, null, null);

            // When
            Feedback result = feedbackMapper.toFeedback(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getNote()).isNull();
            assertThat(result.getComment()).isNull();
            assertThat(result.getBook().getId()).isNull();
        }

        @Test
        @DisplayName("Devrait mapper correctement avec des caractères spéciaux dans le commentaire")
        void shouldMapSpecialCharactersInComment() {
            // Given
            String specialComment = "Commentaire avec émojis 😀 et caractères spéciaux: @#$%^&*()";
            FeedbackRequest request = new FeedbackRequest(4.0, specialComment, 1);

            // When
            Feedback result = feedbackMapper.toFeedback(request);

            // Then
            assertThat(result.getComment()).isEqualTo(specialComment);
        }

        @Test
        @DisplayName("Devrait gérer des conversions multiples avec le même mapper")
        void shouldHandleMultipleConversionsWithSameMapper() {
            // Given
            FeedbackRequest request1 = new FeedbackRequest(4.0, "Comment 1", 1);
            FeedbackRequest request2 = new FeedbackRequest(5.0, "Comment 2", 2);

            // When
            Feedback result1 = feedbackMapper.toFeedback(request1);
            Feedback result2 = feedbackMapper.toFeedback(request2);

            // Then
            assertThat(result1.getNote()).isEqualTo(4.0);
            assertThat(result2.getNote()).isEqualTo(5.0);
            assertThat(result1.getBook().getId()).isEqualTo(1);
            assertThat(result2.getBook().getId()).isEqualTo(2);
        }

        @Test
        @DisplayName("Devrait préserver l'intégrité des données lors du mapping bidirectionnel")
        void shouldPreserveDataIntegrityInBidirectionalMapping() {
            // Given
            FeedbackRequest originalRequest = new FeedbackRequest(4.5, "Test", 10);

            // When
            Feedback mappedFeedback = feedbackMapper.toFeedback(originalRequest);
            mappedFeedback.setCreatedBy(1);
            FeedbackResponse response = feedbackMapper.toFeedbackResponse(mappedFeedback, 1);

            // Then
            assertThat(response.getNote()).isEqualTo(originalRequest.note());
            assertThat(response.getComment()).isEqualTo(originalRequest.comment());
        }
    }
}