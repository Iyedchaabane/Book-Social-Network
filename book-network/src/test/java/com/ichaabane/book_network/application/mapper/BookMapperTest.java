package com.ichaabane.book_network.application.mapper;

import com.ichaabane.book_network.application.dto.request.BookRequest;
import com.ichaabane.book_network.application.dto.response.BookResponse;
import com.ichaabane.book_network.application.dto.response.BorrowedBookResponse;
import com.ichaabane.book_network.domain.model.Book;
import com.ichaabane.book_network.domain.model.BookReservation;
import com.ichaabane.book_network.domain.model.BookTransactionHistory;
import com.ichaabane.book_network.domain.model.Feedback;
import com.ichaabane.book_network.domain.model.User;
import com.ichaabane.book_network.infrastructure.file.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour BookMapper.
 * Couverture : 100%
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookMapper - Tests unitaires")
class BookMapperTest {

    @InjectMocks
    private BookMapper bookMapper;

    private User owner;
    private Book testBook;
    private BookRequest bookRequest;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .build();

        testBook = Book.builder()
                .id(1)
                .title("Clean Code")
                .authorName("Robert C. Martin")
                .isbn("978-0132350884")
                .synopsis("A handbook of agile software craftsmanship")
                .archived(false)
                .shareable(true)
                .owner(owner)
                .bookCover("covers/clean-code.jpg")
                .feedbacks(List.of()) // Liste vide de feedbacks
                .build();

        bookRequest = new BookRequest(
                1,
                "Clean Code",
                "Robert C. Martin",
                "978-0132350884",
                "A handbook of agile software craftsmanship",
                true
        );
    }

    @Nested
    @DisplayName("toBook() - Conversion BookRequest vers Book")
    class ToBookTests {

        @Test
        @DisplayName("Devrait mapper correctement tous les champs du BookRequest")
        void shouldMapAllFieldsFromBookRequest() {
            // When
            Book result = bookMapper.toBook(bookRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getTitle()).isEqualTo("Clean Code");
            assertThat(result.getAuthorName()).isEqualTo("Robert C. Martin");
            assertThat(result.getIsbn()).isEqualTo("978-0132350884");
            assertThat(result.getRate()).isEqualTo(0.0); // Pas de feedbacks
            assertThat(result.getSynopsis()).isEqualTo("A handbook of agile software craftsmanship");
            assertThat(result.isShareable()).isTrue();
        }

        @Test
        @DisplayName("Devrait définir archived à false par défaut")
        void shouldSetArchivedToFalseByDefault() {
            // When
            Book result = bookMapper.toBook(bookRequest);

            // Then
            assertThat(result.isArchived()).isFalse();
        }

        @Test
        @DisplayName("Devrait gérer un BookRequest avec shareable false")
        void shouldHandleNonShareableBook() {
            // Given
            BookRequest request = new BookRequest(
                    2,
                    "Test Book",
                    "Test Author",
                    "123456789",
                    "Synopsis",
                    false
            );

            // When
            Book result = bookMapper.toBook(request);

            // Then
            assertThat(result.isShareable()).isFalse();
        }

        @Test
        @DisplayName("Devrait gérer un BookRequest sans ID")
        void shouldHandleBookRequestWithoutId() {
            // Given
            BookRequest request = new BookRequest(
                    null,
                    "New Book",
                    "Author",
                    "ISBN",
                    "Synopsis",
                    true
            );

            // When
            Book result = bookMapper.toBook(request);

            // Then
            assertThat(result.getId()).isNull();
            assertThat(result.getTitle()).isEqualTo("New Book");
        }

        @Test
        @DisplayName("Devrait gérer un synopsis null")
        void shouldHandleNullSynopsis() {
            // Given
            BookRequest request = new BookRequest(
                    1,
                    "Title",
                    "Author",
                    "ISBN",
                    null,
                    true
            );

            // When
            Book result = bookMapper.toBook(request);

            // Then
            assertThat(result.getSynopsis()).isNull();
        }
    }

    @Nested
    @DisplayName("toBookResponse() - Conversion Book vers BookResponse")
    class ToBookResponseTests {

        @Test
        @DisplayName("Devrait mapper correctement tous les champs du Book")
        void shouldMapAllFieldsFromBook() {
            // Given
            byte[] coverBytes = "coverImageData".getBytes();
            try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
                fileUtilsMock.when(() -> FileUtils.readFileFromLocation("covers/clean-code.jpg"))
                        .thenReturn(coverBytes);

                // When
                BookResponse result = bookMapper.toBookResponse(testBook);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo(1);
                assertThat(result.getTitle()).isEqualTo("Clean Code");
                assertThat(result.getAuthorName()).isEqualTo("Robert C. Martin");
                assertThat(result.getIsbn()).isEqualTo("978-0132350884");
                assertThat(result.getSynopsis()).isEqualTo("A handbook of agile software craftsmanship");
                assertThat(result.isArchived()).isFalse();
                assertThat(result.isShareable()).isTrue();
                assertThat(result.getRate()).isEqualTo(0.0); // Pas de feedbacks = rate 0.0
                assertThat(result.getOwner()).isEqualTo("John Doe");
                assertThat(result.getCover()).isEqualTo(coverBytes);
            }
        }

        @Test
        @DisplayName("Devrait mapper le nom complet du propriétaire")
        void shouldMapOwnerFullName() {
            // Given
            try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
                fileUtilsMock.when(() -> FileUtils.readFileFromLocation(any()))
                        .thenReturn(new byte[0]);

                // When
                BookResponse result = bookMapper.toBookResponse(testBook);

                // Then
                assertThat(result.getOwner()).isEqualTo("John Doe");
            }
        }

        @Test
        @DisplayName("Devrait appeler FileUtils pour lire la couverture")
        void shouldCallFileUtilsToReadCover() {
            // Given
            try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
                fileUtilsMock.when(() -> FileUtils.readFileFromLocation("covers/clean-code.jpg"))
                        .thenReturn(new byte[0]);

                // When
                bookMapper.toBookResponse(testBook);

                // Then
                fileUtilsMock.verify(() -> FileUtils.readFileFromLocation("covers/clean-code.jpg"));
            }
        }

        @Test
        @DisplayName("Devrait gérer un livre archivé")
        void shouldHandleArchivedBook() {
            // Given
            testBook.setArchived(true);
            try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
                fileUtilsMock.when(() -> FileUtils.readFileFromLocation(any()))
                        .thenReturn(new byte[0]);

                // When
                BookResponse result = bookMapper.toBookResponse(testBook);

                // Then
                assertThat(result.isArchived()).isTrue();
            }
        }

        @Test
        @DisplayName("Devrait gérer un livre sans couverture")
        void shouldHandleBookWithoutCover() {
            // Given
            testBook.setBookCover(null);
            try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
                fileUtilsMock.when(() -> FileUtils.readFileFromLocation(null))
                        .thenReturn(null);

                // When
                BookResponse result = bookMapper.toBookResponse(testBook);

                // Then
                assertThat(result.getCover()).isNull();
            }
        }

        @Test
        @DisplayName("Devrait gérer un livre sans note (pas de feedbacks)")
        void shouldHandleBookWithoutRate() {
            // Given - testBook n'a pas de feedbacks
            try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
                fileUtilsMock.when(() -> FileUtils.readFileFromLocation(any()))
                        .thenReturn(new byte[0]);

                // When
                BookResponse result = bookMapper.toBookResponse(testBook);

                // Then - Le rate est 0.0 car pas de feedbacks
                assertThat(result.getRate()).isEqualTo(0.0);
            }
        }
    }

    @Nested
    @DisplayName("toBorrowedBookResponse() - Conversion BookTransactionHistory vers BorrowedBookResponse")
    class ToBorrowedBookResponseTests {

        private BookTransactionHistory transactionHistory;

        @BeforeEach
        void setUp() {
            transactionHistory = BookTransactionHistory.builder()
                    .id(1)
                    .book(testBook)
                    .returned(true)
                    .returnedApproved(true)
                    .build();
        }

        @Test
        @DisplayName("Devrait mapper correctement l'historique de transaction")
        void shouldMapTransactionHistoryCorrectly() {
            // When
            BorrowedBookResponse result = bookMapper.toBorrowedBookResponse(transactionHistory);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getTitle()).isEqualTo("Clean Code");
            assertThat(result.getAuthorName()).isEqualTo("Robert C. Martin");
            assertThat(result.getIsbn()).isEqualTo("978-0132350884");
            assertThat(result.isReturned()).isTrue();
            assertThat(result.isReturnedApproved()).isTrue();
        }

        @Test
        @DisplayName("Devrait gérer un livre non retourné")
        void shouldHandleNotReturnedBook() {
            // Given
            transactionHistory.setReturned(false);
            transactionHistory.setReturnedApproved(false);

            // When
            BorrowedBookResponse result = bookMapper.toBorrowedBookResponse(transactionHistory);

            // Then
            assertThat(result.isReturned()).isFalse();
            assertThat(result.isReturnedApproved()).isFalse();
        }

        @Test
        @DisplayName("Devrait gérer un livre retourné mais non approuvé")
        void shouldHandleReturnedButNotApprovedBook() {
            // Given
            transactionHistory.setReturned(true);
            transactionHistory.setReturnedApproved(false);

            // When
            BorrowedBookResponse result = bookMapper.toBorrowedBookResponse(transactionHistory);

            // Then
            assertThat(result.isReturned()).isTrue();
            assertThat(result.isReturnedApproved()).isFalse();
        }

        @Test
        @DisplayName("Devrait mapper les informations du livre associé")
        void shouldMapAssociatedBookInformation() {
            // When
            BorrowedBookResponse result = bookMapper.toBorrowedBookResponse(transactionHistory);

            // Then
            assertThat(result.getId()).isEqualTo(testBook.getId());
            assertThat(result.getTitle()).isEqualTo(testBook.getTitle());
            assertThat(result.getAuthorName()).isEqualTo(testBook.getAuthorName());
        }
    }

    @Nested
    @DisplayName("toReservationBookResponse() - Conversion BookReservation vers BookResponse")
    class ToReservationBookResponseTests {

        private BookReservation reservation;

        @BeforeEach
        void setUp() {
            reservation = BookReservation.builder()
                    .id(1)
                    .book(testBook)
                    .user(owner)
                    .build();
        }

        @Test
        @DisplayName("Devrait mapper correctement la réservation de livre")
        void shouldMapReservationCorrectly() {
            // Given
            try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
                fileUtilsMock.when(() -> FileUtils.readFileFromLocation(any()))
                        .thenReturn(new byte[0]);

                // When
                BookResponse result = bookMapper.toReservationBookResponse(reservation);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo(1);
                assertThat(result.getTitle()).isEqualTo("Clean Code");
                assertThat(result.getAuthorName()).isEqualTo("Robert C. Martin");
                assertThat(result.getIsbn()).isEqualTo("978-0132350884");
                assertThat(result.getOwner()).isEqualTo("John Doe");
            }
        }

        @Test
        @DisplayName("Devrait utiliser toBookResponse pour la conversion")
        void shouldUsesToBookResponseForConversion() {
            // Given
            try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
                fileUtilsMock.when(() -> FileUtils.readFileFromLocation(any()))
                        .thenReturn(new byte[0]);

                // When
                BookResponse result = bookMapper.toReservationBookResponse(reservation);

                // Then
                // Vérifie que les mêmes champs sont mappés que dans toBookResponse
                assertThat(result.isShareable()).isEqualTo(testBook.isShareable());
                assertThat(result.isArchived()).isEqualTo(testBook.isArchived());
            }
        }

        @Test
        @DisplayName("Devrait extraire le livre de la réservation")
        void shouldExtractBookFromReservation() {
            // Given
            try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
                fileUtilsMock.when(() -> FileUtils.readFileFromLocation(any()))
                        .thenReturn(new byte[0]);

                // When
                BookResponse result = bookMapper.toReservationBookResponse(reservation);

                // Then
                assertThat(result.getId()).isEqualTo(reservation.getBook().getId());
                assertThat(result.getTitle()).isEqualTo(reservation.getBook().getTitle());
            }
        }
    }

    @Nested
    @DisplayName("Cas limites et validation")
    class EdgeCaseTests {

        @Test
        @DisplayName("Devrait gérer un BookRequest avec toutes les valeurs null sauf shareable")
        void shouldHandleBookRequestWithMinimalData() {
            // Given
            BookRequest request = new BookRequest(null, null, null, null, null, true);

            // When
            Book result = bookMapper.toBook(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNull();
            assertThat(result.getTitle()).isNull();
            assertThat(result.getAuthorName()).isNull();
            assertThat(result.getIsbn()).isNull();
            assertThat(result.getSynopsis()).isNull();
            assertThat(result.isShareable()).isTrue();
            assertThat(result.isArchived()).isFalse();
        }

        @Test
        @DisplayName("Devrait gérer des chaînes vides dans BookRequest")
        void shouldHandleEmptyStringsInBookRequest() {
            // Given
            BookRequest request = new BookRequest(1, "", "", "", "", true);

            // When
            Book result = bookMapper.toBook(request);

            // Then
            assertThat(result.getTitle()).isEmpty();
            assertThat(result.getAuthorName()).isEmpty();
            assertThat(result.getIsbn()).isEmpty();
            assertThat(result.getSynopsis()).isEmpty();
        }

        @Test
        @DisplayName("Devrait calculer le rate à partir des feedbacks")
        void shouldCalculateRateFromFeedbacks() {
            // Given
            Feedback feedback1 = Feedback.builder().note(4.0).build();
            Feedback feedback2 = Feedback.builder().note(5.0).build();
            testBook.setFeedbacks(List.of(feedback1, feedback2));
            
            try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
                fileUtilsMock.when(() -> FileUtils.readFileFromLocation(any()))
                        .thenReturn(new byte[0]);

                // When
                BookResponse result = bookMapper.toBookResponse(testBook);

                // Then - Le rate est la moyenne: (4.0 + 5.0) / 2 = 4.5
                assertThat(result.getRate()).isEqualTo(4.5);
            }
        }
        
        @Test
        @DisplayName("Devrait gérer un livre sans couverture")
        void shouldHandleBookWithoutCover() {
            // Given
            testBook.setBookCover(null);
            try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
                fileUtilsMock.when(() -> FileUtils.readFileFromLocation(null))
                        .thenReturn(null);

                // When
                BookResponse result = bookMapper.toBookResponse(testBook);

                // Then
                assertThat(result.getCover()).isNull();
            }
        }
    }
}