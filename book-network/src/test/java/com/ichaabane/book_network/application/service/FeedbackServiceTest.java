package com.ichaabane.book_network.application.service;

import com.ichaabane.book_network.application.dto.request.FeedbackRequest;
import com.ichaabane.book_network.application.dto.response.FeedbackResponse;
import com.ichaabane.book_network.application.dto.response.PageResponse;
import com.ichaabane.book_network.application.mapper.FeedbackMapper;
import com.ichaabane.book_network.domain.exception.OperationNotPermittedException;
import com.ichaabane.book_network.domain.model.Book;
import com.ichaabane.book_network.domain.model.Feedback;
import com.ichaabane.book_network.domain.model.User;
import com.ichaabane.book_network.domain.repository.BookRepository;
import com.ichaabane.book_network.domain.repository.FeedbackRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

/**
 * Tests unitaires pour FeedbackService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackService - Tests unitaires")
class FeedbackServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private FeedbackMapper mapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private FeedbackService feedbackService;

    private User owner;
    private User reviewer;
    private Book testBook;
    private Feedback testFeedback;
    private FeedbackResponse feedbackResponse;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1)
                .firstName("Owner")
                .lastName("User")
                .email("owner@test.com")
                .build();

        reviewer = User.builder()
                .id(2)
                .firstName("Reviewer")
                .lastName("User")
                .email("reviewer@test.com")
                .build();

        testBook = Book.builder()
                .id(1)
                .title("Test Book")
                .authorName("Test Author")
                .shareable(true)
                .archived(false)
                .owner(owner)
                .build();

        testFeedback = Feedback.builder()
                .id(1)
                .note(4.5)
                .comment("Great book!")
                .book(testBook)
                .build();

        feedbackResponse = FeedbackResponse.builder()
                .note(4.5)
                .comment("Great book!")
                .ownFeedback(false)
                .build();
    }

    @Nested
    @DisplayName("saveFeedback() - Sauvegarder un feedback")
    class SaveFeedbackTests {

        @Test
        @DisplayName("Devrait créer un feedback avec succès")
        void shouldCreateFeedbackSuccessfully() {
            // Given
            FeedbackRequest request = new FeedbackRequest(4.5, "Excellent book!", 1);

            given(authentication.getPrincipal()).willReturn(reviewer);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            
            // Créer un nouveau feedback pour le mapper
            Feedback mappedFeedback = Feedback.builder()
                    .note(4.5)
                    .comment("Excellent book!")
                    .book(testBook)
                    .build();
            
            given(mapper.toFeedback(request)).willReturn(mappedFeedback);
            given(feedbackRepository.save(any(Feedback.class))).willReturn(testFeedback);

            // When
            Integer feedbackId = feedbackService.saveFeedback(request, authentication);

            // Then
            assertThat(feedbackId).isNotNull().isEqualTo(1);

            then(bookRepository).should().findById(1);
            then(feedbackRepository).should().save(any(Feedback.class));
            then(mapper).should().toFeedback(request);
        }

        @Test
        @DisplayName("Devrait échouer si le livre n'existe pas")
        void shouldFailWhenBookNotFound() {
            // Given
            FeedbackRequest request = new FeedbackRequest(4.5, "Good", 999);

            given(bookRepository.findById(999)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> feedbackService.saveFeedback(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("No book found with the ID : 999");

            then(feedbackRepository).should(never()).save(any(Feedback.class));
        }

        @Test
        @DisplayName("Devrait échouer si le livre est archivé")
        void shouldFailWhenBookIsArchived() {
            // Given
            testBook.setArchived(true);
            FeedbackRequest request = new FeedbackRequest(4.5, "Comment", 1);

            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));

            // When / Then
            assertThatThrownBy(() -> feedbackService.saveFeedback(request, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("archived");

            then(feedbackRepository).should(never()).save(any(Feedback.class));
        }

        @Test
        @DisplayName("Devrait échouer si le livre n'est pas partageable")
        void shouldFailWhenBookIsNotShareable() {
            // Given
            testBook.setShareable(false);
            FeedbackRequest request = new FeedbackRequest(4.5, "Comment", 1);

            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));

            // When / Then
            assertThatThrownBy(() -> feedbackService.saveFeedback(request, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("not shareable");

            then(feedbackRepository).should(never()).save(any(Feedback.class));
        }

        @Test
        @DisplayName("Devrait échouer si l'utilisateur est le propriétaire")
        void shouldFailWhenUserIsOwner() {
            // Given
            FeedbackRequest request = new FeedbackRequest(4.5, "My book", 1);

            given(authentication.getPrincipal()).willReturn(owner);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));

            // When / Then
            assertThatThrownBy(() -> feedbackService.saveFeedback(request, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("cannot give a feedback to your own book");

            then(feedbackRepository).should(never()).save(any(Feedback.class));
        }

        @Test
        @DisplayName("Devrait gérer un livre archivé ET non partageable")
        void shouldFailWhenBookIsArchivedAndNotShareable() {
            // Given
            testBook.setArchived(true);
            testBook.setShareable(false);
            FeedbackRequest request = new FeedbackRequest(4.5, "Comment", 1);

            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));

            // When / Then
            assertThatThrownBy(() -> feedbackService.saveFeedback(request, authentication))
                    .isInstanceOf(OperationNotPermittedException.class);

            then(feedbackRepository).should(never()).save(any(Feedback.class));
        }
    }

    @Nested
    @DisplayName("findAllFeedbackByBook() - Récupérer feedbacks d'un livre")
    class FindAllFeedbackByBookTests {

        @Test
        @DisplayName("Devrait retourner une page de feedbacks")
        void shouldReturnPageOfFeedbacks() {
            // Given
            given(authentication.getPrincipal()).willReturn(reviewer);

            Page<Feedback> feedbackPage = new PageImpl<>(List.of(testFeedback));
            given(feedbackRepository.findAllByBookId(eq(1), any(Pageable.class)))
                    .willReturn(feedbackPage);
            given(mapper.toFeedbackResponse(any(Feedback.class), anyInt()))
                    .willReturn(feedbackResponse);

            // When
            PageResponse<FeedbackResponse> result = feedbackService.findAllFeedbackByBook(1, 0, 10, authentication);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0))
                    .extracting(FeedbackResponse::getNote, FeedbackResponse::getComment)
                    .containsExactly(4.5, "Great book!");
        }

        @Test
        @DisplayName("Devrait retourner une page vide si aucun feedback")
        void shouldReturnEmptyPageWhenNoFeedbacks() {
            // Given
            given(authentication.getPrincipal()).willReturn(reviewer);

            Page<Feedback> emptyPage = new PageImpl<>(List.of());
            given(feedbackRepository.findAllByBookId(eq(1), any(Pageable.class)))
                    .willReturn(emptyPage);

            // When
            PageResponse<FeedbackResponse> result = feedbackService.findAllFeedbackByBook(1, 0, 10, authentication);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Devrait passer l'ID utilisateur au mapper")
        void shouldPassUserIdToMapper() {
            // Given
            given(authentication.getPrincipal()).willReturn(owner);

            Page<Feedback> feedbackPage = new PageImpl<>(List.of(testFeedback));
            given(feedbackRepository.findAllByBookId(eq(1), any(Pageable.class)))
                    .willReturn(feedbackPage);
            given(mapper.toFeedbackResponse(any(Feedback.class), anyInt()))
                    .willReturn(feedbackResponse);

            // When
            feedbackService.findAllFeedbackByBook(1, 0, 10, authentication);

            // Then
            then(mapper).should().toFeedbackResponse(testFeedback, 1);
        }

        @Test
        @DisplayName("Devrait gérer plusieurs feedbacks")
        void shouldHandleMultipleFeedbacks() {
            // Given
            Feedback feedback2 = Feedback.builder()
                    .id(2)
                    .note(3.0)
                    .comment("Average")
                    .book(testBook)
                    .build();

            FeedbackResponse response2 = FeedbackResponse.builder()
                    .note(3.0)
                    .comment("Average")
                    .ownFeedback(false)
                    .build();

            given(authentication.getPrincipal()).willReturn(reviewer);

            Page<Feedback> feedbackPage = new PageImpl<>(List.of(testFeedback, feedback2));
            given(feedbackRepository.findAllByBookId(eq(1), any(Pageable.class)))
                    .willReturn(feedbackPage);
            given(mapper.toFeedbackResponse(testFeedback, 2))
                    .willReturn(feedbackResponse);
            given(mapper.toFeedbackResponse(feedback2, 2))
                    .willReturn(response2);

            // When
            PageResponse<FeedbackResponse> result = feedbackService.findAllFeedbackByBook(1, 0, 10, authentication);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Devrait respecter la pagination")
        void shouldRespectPagination() {
            // Given
            given(authentication.getPrincipal()).willReturn(reviewer);

            Page<Feedback> feedbackPage = new PageImpl<>(
                    List.of(testFeedback),
                    org.springframework.data.domain.PageRequest.of(2, 5),
                    15
            );
            given(feedbackRepository.findAllByBookId(eq(1), any(Pageable.class)))
                    .willReturn(feedbackPage);
            given(mapper.toFeedbackResponse(testFeedback, 2))
                    .willReturn(feedbackResponse);

            // When
            PageResponse<FeedbackResponse> result = feedbackService.findAllFeedbackByBook(1, 2, 5, authentication);

            // Then
            assertThat(result.getNumber()).isEqualTo(2);
            assertThat(result.getSize()).isEqualTo(5);
            assertThat(result.getTotalElements()).isEqualTo(15);
            assertThat(result.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("Devrait indiquer première et dernière page")
        void shouldIndicateFirstAndLastPage() {
            // Given
            given(authentication.getPrincipal()).willReturn(reviewer);

            Page<Feedback> firstPage = new PageImpl<>(
                    List.of(testFeedback),
                    org.springframework.data.domain.PageRequest.of(0, 10),
                    1
            );
            given(feedbackRepository.findAllByBookId(eq(1), any(Pageable.class)))
                    .willReturn(firstPage);
            given(mapper.toFeedbackResponse(testFeedback, 2))
                    .willReturn(feedbackResponse);

            // When
            PageResponse<FeedbackResponse> result = feedbackService.findAllFeedbackByBook(1, 0, 10, authentication);

            // Then
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }
    }

    @Nested
    @DisplayName("Cas limites et validation")
    class EdgeCaseTests {

        @Test
        @DisplayName("Devrait gérer un ID de livre négatif")
        void shouldHandleNegativeBookId() {
            // Given
            FeedbackRequest request = new FeedbackRequest(4.5, "Comment", -1);

            given(bookRepository.findById(-1)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> feedbackService.saveFeedback(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }
}