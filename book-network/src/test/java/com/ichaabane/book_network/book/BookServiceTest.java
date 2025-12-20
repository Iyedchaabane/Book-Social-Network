package com.ichaabane.book_network.book;

import com.ichaabane.book_network.exception.OperationNotPermittedException;
import com.ichaabane.book_network.history.BookTransactionHistory;
import com.ichaabane.book_network.history.BookTransactionHistoryRepository;
import com.ichaabane.book_network.notification.NotificationService;
import com.ichaabane.book_network.notification.NotificationStatus;
import com.ichaabane.book_network.reservation.BookReservation;
import com.ichaabane.book_network.reservation.BookReservationRepository;
import com.ichaabane.book_network.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookTransactionHistoryRepository transactionHistoryRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private BookReservationRepository reservationRepository;

    @InjectMocks
    private BookService bookService;

    private User owner;
    private User borrower;
    private Book book;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1);

        borrower = new User();
        borrower.setId(2);

        book = new Book();
        book.setId(10);
        book.setArchived(false);
        book.setShareable(true);
        book.setOwner(owner);
        book.setCreatedBy(owner.getId());
    }

    private Authentication mockAuth(User user) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        return auth;
    }

    @Test
    void borrowBook_ShouldSucceed_WhenBookIsAvailable() {
        // given
        when(bookRepository.findById(10)).thenReturn(Optional.of(book));
        when(transactionHistoryRepository.isAlreadyBorrowedByUser(10, borrower.getId())).thenReturn(false);

        Authentication auth = mockAuth(borrower);

        BookTransactionHistory savedHistory = new BookTransactionHistory();
        savedHistory.setId(100);
        when(transactionHistoryRepository.save(any())).thenReturn(savedHistory);

        // when
        Integer result = bookService.borrowBook(10, auth);

        // then
        assertEquals(100, result);
        verify(transactionHistoryRepository).save(any(BookTransactionHistory.class));
        verify(notificationService).sendNotification(
                eq(owner),
                any(NotificationStatus.class),
                anyString(),
                any()
        );
    }

    @Test
    void borrowBook_ShouldFail_WhenBookArchived() {
        // given
        book.setArchived(true);
        when(bookRepository.findById(10)).thenReturn(Optional.of(book));

        // when
        Authentication auth = mockAuth(borrower);

        // then
        assertThrows(OperationNotPermittedException.class,
                () -> bookService.borrowBook(10, auth));
    }

    @Test
    void borrowBook_ShouldFail_WhenUserIsOwner() {
        // given
        when(bookRepository.findById(10)).thenReturn(Optional.of(book));

        Authentication auth = mockAuth(owner);

        // then
        assertThrows(OperationNotPermittedException.class,
                () -> bookService.borrowBook(10, auth));
    }

    @Test
    void returnBorrowBook_ShouldSucceed_WhenBorrowerReturnsBook() {
        // given
        BookTransactionHistory history = new BookTransactionHistory();
        history.setId(200);
        history.setBook(book);
        history.setUser(borrower);
        history.setReturned(false);

        when(bookRepository.findById(10)).thenReturn(Optional.of(book));
        when(transactionHistoryRepository.findByBookIdAndUserId(10, borrower.getId()))
                .thenReturn(Optional.of(history));

        Authentication auth = mockAuth(borrower);

        when(transactionHistoryRepository.save(any())).thenAnswer(invocation -> {
            BookTransactionHistory saved = invocation.getArgument(0);
            saved.setId(200);
            return saved;
        });

        // when
        Integer result = bookService.returnBorrowBook(10, auth);

        // then
        assertEquals(200, result);
        assertTrue(history.isReturned());
        verify(notificationService).sendNotification(
                eq(owner),
                any(NotificationStatus.class),
                anyString(),
                any()
        );
    }

    @Test
    void returnBorrowBook_ShouldFail_WhenNoBorrowHistoryFound() {
        // given
        when(bookRepository.findById(10)).thenReturn(Optional.of(book));
        when(transactionHistoryRepository.findByBookIdAndUserId(10, borrower.getId()))
                .thenReturn(Optional.empty());

        Authentication auth = mockAuth(borrower);

        // then
        assertThrows(OperationNotPermittedException.class,
                () -> bookService.returnBorrowBook(10, auth));
    }

    @Test
    void approveReturnBorrowBook_ShouldSucceed_WhenOwnerApprovesReturn() {
        // given
        BookTransactionHistory history = new BookTransactionHistory();
        history.setId(300);
        history.setBook(book);
        history.setUser(borrower);
        history.setReturned(true);
        history.setReturnedApproved(false);

        when(bookRepository.findById(10)).thenReturn(Optional.of(book));
        when(transactionHistoryRepository.findByBookIdAndOwnerId(10, owner.getId()))
                .thenReturn(Optional.of(history));

        Authentication auth = mockAuth(owner);

        when(transactionHistoryRepository.save(any())).thenAnswer(invocation -> {
            BookTransactionHistory saved = invocation.getArgument(0);
            saved.setReturnedApproved(true);
            return saved;
        });

        // when
        Integer result = bookService.approveReturnBorrowBook(10, auth);

        // then
        assertEquals(300, result);
        assertTrue(history.isReturnedApproved());
        verify(notificationService).sendNotification(
                eq(history.getUser()),
                any(NotificationStatus.class),
                anyString(),
                any()
        );
    }

    @Test
    void approveReturnBorrowBook_ShouldFail_WhenUserIsNotOwner() {
        // given
        when(bookRepository.findById(10)).thenReturn(Optional.of(book));

        Authentication auth = mockAuth(borrower);

        // then
        assertThrows(OperationNotPermittedException.class,
                () -> bookService.approveReturnBorrowBook(10, auth));
    }

    @Test
    void addReservation_ShouldSucceed_WhenBookIsBorrowedAndNotOwned() {
        // given
        when(bookRepository.findById(10)).thenReturn(Optional.of(book));
        when(transactionHistoryRepository.existsByBookIdAndReturnedFalse(10)).thenReturn(true);
        when(reservationRepository.existsByBookAndUser(book, borrower)).thenReturn(false);

        Authentication auth = mockAuth(borrower);

        BookReservation savedReservation = BookReservation.builder().id(100).book(book).user(borrower).build();
        when(reservationRepository.save(any())).thenReturn(savedReservation);

        // when
        bookService.addReservation(10, auth);

        // then
        verify(reservationRepository).save(any(BookReservation.class));
        verify(notificationService).sendNotification(
                eq(owner),
                any(NotificationStatus.class),
                anyString(),
                any()
        );
    }

    @Test
    void addReservation_ShouldFail_WhenBookIsArchived() {
        book.setArchived(true);
        when(bookRepository.findById(10)).thenReturn(Optional.of(book));
        Authentication auth = mockAuth(borrower);

        assertThrows(OperationNotPermittedException.class,
                () -> bookService.addReservation(10, auth));
    }

    @Test
    void addReservation_ShouldFail_WhenUserIsOwner() {
        when(bookRepository.findById(10)).thenReturn(Optional.of(book));
        Authentication auth = mockAuth(owner);

        assertThrows(OperationNotPermittedException.class,
                () -> bookService.addReservation(10, auth));
    }

    @Test
    void addReservation_ShouldFail_WhenAlreadyReserved() {
        when(bookRepository.findById(10)).thenReturn(Optional.of(book));
        when(reservationRepository.existsByBookAndUser(book, borrower)).thenReturn(true);
        when(transactionHistoryRepository.existsByBookIdAndReturnedFalse(10)).thenReturn(true);

        Authentication auth = mockAuth(borrower);

        assertThrows(OperationNotPermittedException.class,
                () -> bookService.addReservation(10, auth));
    }

    @Test
    void addReservation_ShouldFail_WhenBookIsNotBorrowed() {
        when(bookRepository.findById(10)).thenReturn(Optional.of(book));
        when(transactionHistoryRepository.existsByBookIdAndReturnedFalse(10)).thenReturn(false);

        Authentication auth = mockAuth(borrower);

        assertThrows(OperationNotPermittedException.class,
                () -> bookService.addReservation(10, auth));
    }

    @Test
    void removeReservation_ShouldSucceed_WhenReservationExists() {
        BookReservation reservation = BookReservation.builder().id(101).book(book).user(borrower).build();

        when(reservationRepository.findByBookIdAndUserId(10, borrower.getId()))
                .thenReturn(Optional.of(reservation));

        Authentication auth = mockAuth(borrower);

        bookService.removeReservation(10, auth);

        verify(reservationRepository).delete(reservation);
        verify(notificationService).sendNotification(
                eq(book.getOwner()),
                any(NotificationStatus.class),
                anyString(),
                any()
        );
    }

    @Test
    void removeReservation_ShouldFail_WhenReservationDoesNotExist() {
        when(reservationRepository.findByBookIdAndUserId(10, borrower.getId()))
                .thenReturn(Optional.empty());

        Authentication auth = mockAuth(borrower);

        assertThrows(EntityNotFoundException.class,
                () -> bookService.removeReservation(10, auth));
    }
}