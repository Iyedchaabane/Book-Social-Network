package com.ichaabane.book_network.application.service;

import com.ichaabane.book_network.application.dto.request.BookRequest;
import com.ichaabane.book_network.application.dto.response.BookResponse;
import com.ichaabane.book_network.application.dto.response.BorrowedBookResponse;
import com.ichaabane.book_network.application.dto.response.PageResponse;
import com.ichaabane.book_network.application.mapper.BookMapper;
import com.ichaabane.book_network.domain.exception.OperationNotPermittedException;
import com.ichaabane.book_network.domain.model.*;
import com.ichaabane.book_network.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static com.ichaabane.book_network.domain.enums.NotificationStatus.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests unitaires pour BookService.
 * 
 * Couverture complète de la logique métier sans contexte Spring.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BookService - Tests unitaires")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookTransactionHistoryRepository transactionHistoryRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private BookReservationRepository reservationRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BookService bookService;

    private User owner;
    private User borrower;
    private Book testBook;
    private BookResponse bookResponse;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1)
                .firstName("Owner")
                .lastName("User")
                .email("owner@test.com")
                .build();

        borrower = User.builder()
                .id(2)
                .firstName("Borrower")
                .lastName("User")
                .email("borrower@test.com")
                .build();

        testBook = Book.builder()
                .id(1)
                .title("Test Book")
                .authorName("Test Author")
                .isbn("ISBN123")
                .synopsis("Test Synopsis")
                .shareable(true)
                .archived(false)
                .owner(owner)
                .build();

        bookResponse = BookResponse.builder()
                .id(1)
                .title("Test Book")
                .authorName("Test Author")
                .isbn("ISBN123")
                .build();
    }

    @Nested
    @DisplayName("save() - Sauvegarder/Mettre à jour un livre")
    class SaveTests {

        @Test
        @DisplayName("Devrait créer un nouveau livre")
        void shouldCreateNewBook() {
            // Given
            BookRequest request = new BookRequest(
                    null, "New Book", "Author", "ISBN456", "Synopsis", true
            );
            
            Book newBook = Book.builder()
                    .title("New Book")
                    .authorName("Author")
                    .build();

            given(authentication.getPrincipal()).willReturn(owner);
            given(bookMapper.toBook(request)).willReturn(newBook);
            given(bookRepository.save(any(Book.class))).willAnswer(invocation -> {
                Book book = invocation.getArgument(0);
                book.setId(10);
                return book;
            });

            // When
            Integer bookId = bookService.save(request, authentication);

            // Then
            assertThat(bookId).isEqualTo(10);
            
            ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
            then(bookRepository).should().save(bookCaptor.capture());
            assertThat(bookCaptor.getValue().getOwner()).isEqualTo(owner);
        }

        @Test
        @DisplayName("Devrait mettre à jour un livre existant")
        void shouldUpdateExistingBook() {
            // Given
            BookRequest request = new BookRequest(
                    1, "Updated Title", "Updated Author", "ISBN123", "Updated Synopsis", false
            );

            given(authentication.getPrincipal()).willReturn(owner);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(bookRepository.save(any(Book.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            Integer bookId = bookService.save(request, authentication);

            // Then
            assertThat(bookId).isEqualTo(1);
            assertThat(testBook.getTitle()).isEqualTo("Updated Title");
            assertThat(testBook.getAuthorName()).isEqualTo("Updated Author");
            assertThat(testBook.isShareable()).isFalse();
        }

        @Test
        @DisplayName("Devrait échouer si le livre à mettre à jour n'existe pas")
        void shouldFailWhenBookNotFound() {
            // Given
            BookRequest request = new BookRequest(
                    999, "Updated Title", "Author", "ISBN", "Synopsis", true
            );

            given(authentication.getPrincipal()).willReturn(owner);
            given(bookRepository.findById(999)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> bookService.save(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Book not found");
        }
    }

    @Nested
    @DisplayName("findBookById() - Trouver un livre par ID")
    class FindBookByIdTests {

        @Test
        @DisplayName("Devrait retourner le livre trouvé")
        void shouldReturnBookWhenFound() {
            // Given
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(bookMapper.toBookResponse(testBook)).willReturn(bookResponse);

            // When
            BookResponse result = bookService.findBookById(1);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getTitle()).isEqualTo("Test Book");
        }

        @Test
        @DisplayName("Devrait échouer si le livre n'existe pas")
        void shouldFailWhenBookNotFound() {
            // Given
            given(bookRepository.findById(999)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> bookService.findBookById(999))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("No book found with the ID : 999");
        }
    }

    @Nested
    @DisplayName("findAllBooks() - Lister tous les livres affichables")
    class FindAllBooksTests {

        @Test
        @DisplayName("Devrait retourner une page de livres")
        void shouldReturnPageOfBooks() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            
            Page<Book> bookPage = new PageImpl<>(List.of(testBook), PageRequest.of(0, 10), 1);
            given(bookRepository.findAllDisplayableBooks(any(Pageable.class), eq(2)))
                    .willReturn(bookPage);
            given(bookMapper.toBookResponse(testBook)).willReturn(bookResponse);

            // When
            PageResponse<BookResponse> result = bookService.findAllBooks(0, 10, authentication);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }

        @Test
        @DisplayName("Devrait retourner une page vide si aucun livre")
        void shouldReturnEmptyPageWhenNoBooks() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            
            Page<Book> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            given(bookRepository.findAllDisplayableBooks(any(Pageable.class), eq(2)))
                    .willReturn(emptyPage);

            // When
            PageResponse<BookResponse> result = bookService.findAllBooks(0, 10, authentication);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("findAllBooksByOwner() - Livres d'un propriétaire")
    class FindAllBooksByOwnerTests {

        @Test
        @DisplayName("Devrait retourner les livres du propriétaire")
        void shouldReturnOwnerBooks() {
            // Given
            given(authentication.getPrincipal()).willReturn(owner);
            
            Page<Book> bookPage = new PageImpl<>(List.of(testBook), PageRequest.of(0, 10), 1);
            given(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .willReturn(bookPage);
            given(bookMapper.toBookResponse(testBook)).willReturn(bookResponse);

            // When
            PageResponse<BookResponse> result = bookService.findAllBooksByOwner(0, 10, authentication);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findAllBorrowedBooks() - Livres empruntés par utilisateur")
    class FindAllBorrowedBooksTests {

        @Test
        @DisplayName("Devrait retourner les livres empruntés")
        void shouldReturnBorrowedBooks() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            
            BookTransactionHistory transaction = BookTransactionHistory.builder()
                    .id(1)
                    .book(testBook)
                    .user(borrower)
                    .returned(false)
                    .build();
            
            BorrowedBookResponse borrowedResponse = BorrowedBookResponse.builder()
                    .id(1)
                    .title("Test Book")
                    .build();

            Page<BookTransactionHistory> transactionPage = new PageImpl<>(List.of(transaction));
            given(transactionHistoryRepository.findAllBorrowedBooks(any(Pageable.class), eq(2)))
                    .willReturn(transactionPage);
            given(bookMapper.toBorrowedBookResponse(transaction)).willReturn(borrowedResponse);

            // When
            PageResponse<BorrowedBookResponse> result = bookService.findAllBorrowedBooks(0, 10, authentication);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findAllReturnedBooks() - Livres retournés")
    class FindAllReturnedBooksTests {

        @Test
        @DisplayName("Devrait retourner les livres retournés")
        void shouldReturnReturnedBooks() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            
            BookTransactionHistory transaction = BookTransactionHistory.builder()
                    .id(1)
                    .book(testBook)
                    .user(borrower)
                    .returned(true)
                    .build();
            
            BorrowedBookResponse borrowedResponse = BorrowedBookResponse.builder()
                    .id(1)
                    .title("Test Book")
                    .build();

            Page<BookTransactionHistory> transactionPage = new PageImpl<>(List.of(transaction));
            given(transactionHistoryRepository.findAllReturnedBooks(any(Pageable.class), eq(2)))
                    .willReturn(transactionPage);
            given(bookMapper.toBorrowedBookResponse(transaction)).willReturn(borrowedResponse);

            // When
            PageResponse<BorrowedBookResponse> result = bookService.findAllReturnedBooks(0, 10, authentication);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("updateShareableStatus() - Mise à jour du statut partageable")
    class UpdateShareableStatusTests {

        @Test
        @DisplayName("Devrait basculer le statut partageable")
        void shouldToggleShareableStatus() {
            // Given
            given(authentication.getPrincipal()).willReturn(owner);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(bookRepository.save(any(Book.class))).willAnswer(invocation -> invocation.getArgument(0));

            boolean initialStatus = testBook.isShareable();

            // When
            Integer result = bookService.updateShareableStatus(1, authentication);

            // Then
            assertThat(result).isEqualTo(1);
            assertThat(testBook.isShareable()).isNotEqualTo(initialStatus);
        }

        @Test
        @DisplayName("Devrait échouer si l'utilisateur n'est pas le propriétaire")
        void shouldFailWhenUserIsNotOwner() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));

            // When / Then
            assertThatThrownBy(() -> bookService.updateShareableStatus(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("cannot update others book shareable status");
        }

        @Test
        @DisplayName("Devrait échouer si le livre n'existe pas")
        void shouldFailWhenBookNotFound() {
            // Given
            given(authentication.getPrincipal()).willReturn(owner);
            given(bookRepository.findById(999)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> bookService.updateShareableStatus(999, authentication))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateArchivedStatus() - Mise à jour du statut archivé")
    class UpdateArchivedStatusTests {

        @Test
        @DisplayName("Devrait basculer le statut archivé")
        void shouldToggleArchivedStatus() {
            // Given
            given(authentication.getPrincipal()).willReturn(owner);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(bookRepository.save(any(Book.class))).willAnswer(invocation -> invocation.getArgument(0));

            boolean initialStatus = testBook.isArchived();

            // When
            Integer result = bookService.updateArchivedStatus(1, authentication);

            // Then
            assertThat(result).isEqualTo(1);
            assertThat(testBook.isArchived()).isNotEqualTo(initialStatus);
        }

        @Test
        @DisplayName("Devrait échouer si l'utilisateur n'est pas le propriétaire")
        void shouldFailWhenUserIsNotOwner() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));

            // When / Then
            assertThatThrownBy(() -> bookService.updateArchivedStatus(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("cannot update others book archived status");
        }
    }

    @Nested
    @DisplayName("borrowBook() - Emprunter un livre")
    class BorrowBookTests {

        @Test
        @DisplayName("Devrait emprunter un livre avec succès")
        void shouldBorrowBookSuccessfully() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(transactionHistoryRepository.isAlreadyBorrowedByUser(1, 2)).willReturn(false);
            given(transactionHistoryRepository.existsByBookIdAndReturnedFalse(1)).willReturn(false);
            given(transactionHistoryRepository.save(any(BookTransactionHistory.class))).willAnswer(invocation -> {
                BookTransactionHistory history = invocation.getArgument(0);
                history.setId(100);
                return history;
            });
            willDoNothing().given(notificationService).sendNotification(any(), any(), anyString(), anyString());

            // When
            Integer transactionId = bookService.borrowBook(1, authentication);

            // Then
            assertThat(transactionId).isEqualTo(100);
            
            ArgumentCaptor<BookTransactionHistory> historyCaptor = ArgumentCaptor.forClass(BookTransactionHistory.class);
            then(transactionHistoryRepository).should().save(historyCaptor.capture());
            
            BookTransactionHistory saved = historyCaptor.getValue();
            assertThat(saved.getBook()).isEqualTo(testBook);
            assertThat(saved.getUser()).isEqualTo(borrower);
            assertThat(saved.isReturned()).isFalse();

            then(notificationService).should().sendNotification(
                    eq(owner),
                    eq(BORROWED),
                    eq("Your book has been borrowed"),
                    eq("Test Book")
            );
        }

        @Test
        @DisplayName("Devrait échouer si le livre est archivé")
        void shouldFailWhenBookIsArchived() {
            // Given
            testBook.setArchived(true);
            given(authentication.getPrincipal()).willReturn(borrower);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));

            // When / Then
            assertThatThrownBy(() -> bookService.borrowBook(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("cannot be borrowed");
        }

        @Test
        @DisplayName("Devrait échouer si le livre n'est pas partageable")
        void shouldFailWhenBookIsNotShareable() {
            // Given
            testBook.setShareable(false);
            given(authentication.getPrincipal()).willReturn(borrower);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));

            // When / Then
            assertThatThrownBy(() -> bookService.borrowBook(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("cannot be borrowed");
        }

        @Test
        @DisplayName("Devrait échouer si l'utilisateur est le propriétaire")
        void shouldFailWhenUserIsOwner() {
            // Given
            given(authentication.getPrincipal()).willReturn(owner);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));

            // When / Then
            assertThatThrownBy(() -> bookService.borrowBook(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("cannot borrow your own book");
        }

        @Test
        @DisplayName("Devrait échouer si déjà emprunté par l'utilisateur")
        void shouldFailWhenAlreadyBorrowedByUser() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(transactionHistoryRepository.isAlreadyBorrowedByUser(1, 2)).willReturn(true);

            // When / Then
            assertThatThrownBy(() -> bookService.borrowBook(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("already borrowed");
        }

        @Test
        @DisplayName("Devrait échouer si déjà emprunté par quelqu'un d'autre")
        void shouldFailWhenAlreadyBorrowedByOther() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(transactionHistoryRepository.isAlreadyBorrowedByUser(1, 2)).willReturn(false);
            given(transactionHistoryRepository.existsByBookIdAndReturnedFalse(1)).willReturn(true);

            // When / Then
            assertThatThrownBy(() -> bookService.borrowBook(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("borrowed by another user");
        }
    }

    @Nested
    @DisplayName("returnBorrowBook() - Retourner un livre emprunté")
    class ReturnBorrowBookTests {

        @Test
        @DisplayName("Devrait retourner un livre emprunté")
        void shouldReturnBorrowedBook() {
            // Given
            BookTransactionHistory transaction = BookTransactionHistory.builder()
                    .id(100)
                    .book(testBook)
                    .user(borrower)
                    .returned(false)
                    .build();

            given(authentication.getPrincipal()).willReturn(borrower);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(transactionHistoryRepository.findByBookIdAndUserId(1, 2))
                    .willReturn(Optional.of(transaction));
            given(transactionHistoryRepository.save(any(BookTransactionHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            willDoNothing().given(notificationService).sendNotification(any(), any(), anyString(), anyString());

            // When
            Integer result = bookService.returnBorrowBook(1, authentication);

            // Then
            assertThat(result).isEqualTo(100);
            assertThat(transaction.isReturned()).isTrue();

            then(notificationService).should().sendNotification(
                    eq(owner),
                    eq(RETURNED),
                    eq("Your book has been returned"),
                    eq("Test Book")
            );
        }

        @Test
        @DisplayName("Devrait échouer si le livre n'a pas été emprunté")
        void shouldFailWhenBookNotBorrowed() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(transactionHistoryRepository.findByBookIdAndUserId(1, 2))
                    .willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> bookService.returnBorrowBook(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("You did not borrow this book");
        }

        @Test
        @DisplayName("Devrait échouer si l'utilisateur est le propriétaire")
        void shouldFailWhenUserIsOwner() {
            // Given
            given(authentication.getPrincipal()).willReturn(owner);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));

            // When / Then
            assertThatThrownBy(() -> bookService.returnBorrowBook(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("cannot borrow your own book");
        }
    }

    @Nested
    @DisplayName("approveReturnBorrowBook() - Approuver le retour")
    class ApproveReturnTests {

        @Test
        @DisplayName("Devrait approuver le retour d'un livre")
        void shouldApproveBookReturn() {
            // Given
            BookTransactionHistory transaction = BookTransactionHistory.builder()
                    .id(100)
                    .book(testBook)
                    .user(borrower)
                    .returned(true)
                    .returnedApproved(false)
                    .build();

            given(authentication.getPrincipal()).willReturn(owner);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(transactionHistoryRepository.findByBookIdAndOwnerId(1, 1))
                    .willReturn(Optional.of(transaction));
            given(transactionHistoryRepository.save(any(BookTransactionHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            willDoNothing().given(notificationService).sendNotification(any(), any(), anyString(), anyString());

            // When
            Integer result = bookService.approveReturnBorrowBook(1, authentication);

            // Then
            assertThat(result).isEqualTo(100);
            assertThat(transaction.isReturnedApproved()).isTrue();

            then(notificationService).should().sendNotification(
                    eq(borrower),
                    eq(RETURN_APPROVED),
                    eq("You book return has been approved"),
                    eq("Test Book")
            );
        }

        @Test
        @DisplayName("Devrait échouer si l'utilisateur n'est pas le propriétaire")
        void shouldFailWhenUserIsNotOwner() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));

            // When / Then
            assertThatThrownBy(() -> bookService.approveReturnBorrowBook(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("cannot return a book that you do do not own");
        }

        @Test
        @DisplayName("Devrait échouer si le livre n'est pas retourné")
        void shouldFailWhenBookNotReturned() {
            // Given
            given(authentication.getPrincipal()).willReturn(owner);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(transactionHistoryRepository.findByBookIdAndOwnerId(1, 1))
                    .willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> bookService.approveReturnBorrowBook(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("not returned yet");
        }
    }

    @Nested
    @DisplayName("uploadCover() - Téléverser une couverture")
    class UploadCoverTests {

        @Mock
        private MultipartFile mockFile;

        @Test
        @DisplayName("Devrait téléverser la couverture avec succès")
        void shouldUploadCoverSuccessfully() {
            // Given
            given(authentication.getPrincipal()).willReturn(owner);
            given(mockFile.isEmpty()).willReturn(false);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(fileStorageService.saveFile(mockFile, 1)).willReturn("/path/to/cover.jpg");
            given(bookRepository.save(any(Book.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            bookService.uploadCover(mockFile, authentication, 1);

            // Then
            assertThat(testBook.getBookCover()).isEqualTo("/path/to/cover.jpg");
            then(bookRepository).should().save(testBook);
        }

        @Test
        @DisplayName("Ne devrait rien faire si le fichier est null")
        void shouldDoNothingWhenFileIsNull() {
            // When
            bookService.uploadCover(null, authentication, 1);

            // Then
            then(bookRepository).should(never()).findById(anyInt());
            then(fileStorageService).should(never()).saveFile(any(), anyInt());
        }

        @Test
        @DisplayName("Ne devrait rien faire si le fichier est vide")
        void shouldDoNothingWhenFileIsEmpty() {
            // Given
            given(mockFile.isEmpty()).willReturn(true);

            // When
            bookService.uploadCover(mockFile, authentication, 1);

            // Then
            then(bookRepository).should(never()).findById(anyInt());
            then(fileStorageService).should(never()).saveFile(any(), anyInt());
        }

        @Test
        @DisplayName("Devrait échouer si le livre n'existe pas")
        void shouldFailWhenBookNotFound() {
            // Given
            given(authentication.getPrincipal()).willReturn(owner);
            given(mockFile.isEmpty()).willReturn(false);
            given(bookRepository.findById(999)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> bookService.uploadCover(mockFile, authentication, 999))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("addReservation() - Ajouter une réservation")
    class AddReservationTests {

        @Test
        @DisplayName("Devrait créer une réservation avec succès")
        void shouldCreateReservationSuccessfully() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(transactionHistoryRepository.isAlreadyBorrowedByUser(1, 2)).willReturn(false);
            given(reservationRepository.existsByBookAndUser(testBook, borrower)).willReturn(false);
            given(transactionHistoryRepository.existsByBookIdAndReturnedFalse(1)).willReturn(true);
            given(reservationRepository.save(any(BookReservation.class))).willAnswer(invocation -> invocation.getArgument(0));
            willDoNothing().given(notificationService).sendNotification(any(), any(), anyString(), anyString());

            // When
            bookService.addReservation(1, authentication);

            // Then
            ArgumentCaptor<BookReservation> reservationCaptor = ArgumentCaptor.forClass(BookReservation.class);
            then(reservationRepository).should().save(reservationCaptor.capture());
            
            BookReservation saved = reservationCaptor.getValue();
            assertThat(saved.getBook()).isEqualTo(testBook);
            assertThat(saved.getUser()).isEqualTo(borrower);

            then(notificationService).should().sendNotification(
                    eq(owner),
                    eq(RESERVED),
                    anyString(),
                    eq("Test Book")
            );
        }

        @Test
        @DisplayName("Devrait échouer si le livre est archivé")
        void shouldFailWhenBookIsArchived() {
            // Given
            testBook.setArchived(true);
            given(authentication.getPrincipal()).willReturn(borrower);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));

            // When / Then
            assertThatThrownBy(() -> bookService.addReservation(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("Archived book cannot be reserved");
        }

        @Test
        @DisplayName("Devrait échouer si l'utilisateur est le propriétaire")
        void shouldFailWhenUserIsOwner() {
            // Given
            given(authentication.getPrincipal()).willReturn(owner);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));

            // When / Then
            assertThatThrownBy(() -> bookService.addReservation(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("cannot reserve a book that you do own");
        }

        @Test
        @DisplayName("Devrait échouer si déjà emprunté par l'utilisateur")
        void shouldFailWhenAlreadyBorrowed() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(transactionHistoryRepository.isAlreadyBorrowedByUser(1, 2)).willReturn(true);

            // When / Then
            assertThatThrownBy(() -> bookService.addReservation(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("already borrowed this book");
        }

        @Test
        @DisplayName("Devrait échouer si déjà réservé")
        void shouldFailWhenAlreadyReserved() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(transactionHistoryRepository.isAlreadyBorrowedByUser(1, 2)).willReturn(false);
            given(reservationRepository.existsByBookAndUser(testBook, borrower)).willReturn(true);

            // When / Then
            assertThatThrownBy(() -> bookService.addReservation(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("already reserved this book");
        }

        @Test
        @DisplayName("Devrait échouer si le livre est disponible")
        void shouldFailWhenBookIsAvailable() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            given(bookRepository.findById(1)).willReturn(Optional.of(testBook));
            given(transactionHistoryRepository.isAlreadyBorrowedByUser(1, 2)).willReturn(false);
            given(reservationRepository.existsByBookAndUser(testBook, borrower)).willReturn(false);
            given(transactionHistoryRepository.existsByBookIdAndReturnedFalse(1)).willReturn(false);

            // When / Then
            assertThatThrownBy(() -> bookService.addReservation(1, authentication))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("currently available");
        }
    }

    @Nested
    @DisplayName("removeReservation() - Supprimer une réservation")
    class RemoveReservationTests {

        @Test
        @DisplayName("Devrait supprimer une réservation avec succès")
        void shouldRemoveReservationSuccessfully() {
            // Given
            BookReservation reservation = BookReservation.builder()
                    .id(1)
                    .book(testBook)
                    .user(borrower)
                    .build();

            given(authentication.getPrincipal()).willReturn(borrower);
            given(reservationRepository.findByBookIdAndUserId(1, 2)).willReturn(Optional.of(reservation));
            willDoNothing().given(reservationRepository).delete(reservation);
            willDoNothing().given(notificationService).sendNotification(any(), any(), anyString(), anyString());

            // When
            bookService.removeReservation(1, authentication);

            // Then
            then(reservationRepository).should().delete(reservation);
            then(notificationService).should().sendNotification(
                    eq(owner),
                    eq(CANCELLED),
                    anyString(),
                    eq("Test Book")
            );
        }

        @Test
        @DisplayName("Devrait échouer si la réservation n'existe pas")
        void shouldFailWhenReservationNotFound() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            given(reservationRepository.findByBookIdAndUserId(1, 2)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> bookService.removeReservation(1, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("No reservation found");
        }
    }

    @Nested
    @DisplayName("getUserReservations() - Obtenir les réservations d'un utilisateur")
    class GetUserReservationsTests {

        @Test
        @DisplayName("Devrait retourner les réservations de l'utilisateur")
        void shouldReturnUserReservations() {
            // Given
            BookReservation reservation = BookReservation.builder()
                    .id(1)
                    .book(testBook)
                    .user(borrower)
                    .build();

            given(authentication.getPrincipal()).willReturn(borrower);
            
            Page<BookReservation> reservationPage = new PageImpl<>(List.of(reservation));
            given(reservationRepository.findAllByUserId(eq(2), any(Pageable.class)))
                    .willReturn(reservationPage);
            given(bookMapper.toReservationBookResponse(reservation)).willReturn(bookResponse);

            // When
            PageResponse<BookResponse> result = bookService.getUserReservations(0, 10, authentication);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Devrait retourner une page vide si aucune réservation")
        void shouldReturnEmptyPageWhenNoReservations() {
            // Given
            given(authentication.getPrincipal()).willReturn(borrower);
            
            Page<BookReservation> emptyPage = new PageImpl<>(List.of());
            given(reservationRepository.findAllByUserId(eq(2), any(Pageable.class)))
                    .willReturn(emptyPage);

            // When
            PageResponse<BookResponse> result = bookService.getUserReservations(0, 10, authentication);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }
    }
}
