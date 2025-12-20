package com.ichaabane.book_network.book;

import com.ichaabane.book_network.common.PageResponse;
import com.ichaabane.book_network.exception.OperationNotPermittedException;
import com.ichaabane.book_network.file.FileStorageService;
import com.ichaabane.book_network.history.BookTransactionHistory;
import com.ichaabane.book_network.history.BookTransactionHistoryRepository;
import com.ichaabane.book_network.notification.NotificationService;
import com.ichaabane.book_network.reservation.BookReservation;
import com.ichaabane.book_network.reservation.BookReservationRepository;
import com.ichaabane.book_network.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

import static com.ichaabane.book_network.book.BookSpecification.*;
import static com.ichaabane.book_network.notification.NotificationStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final BookTransactionHistoryRepository transactionHistoryRepository;
    private final BookMapper bookMapper;
    private final BookTransactionHistoryRepository bookTransactionHistoryRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final BookReservationRepository reservationRepository;

    public Integer save(BookRequest request, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Book book;

        if (request.id() != null) {
            book = bookRepository.findById(request.id())
                    .orElseThrow(() -> new EntityNotFoundException("Book not found"));
            book.setTitle(request.title());
            book.setAuthorName(request.authorName());
            book.setIsbn(request.isbn());
            book.setSynopsis(request.synopsis());
            book.setShareable(request.shareable());
        } else {
            book = bookMapper.toBook(request);
            book.setOwner(user);
        }

        return bookRepository.save(book).getId();
    }

    public BookResponse findBookById(Integer bookId) {
        return bookRepository.findById(bookId)
                .map(bookMapper::toBookResponse)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the ID : " + bookId));
    }

    public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAllDisplayableBooks(pageable, user.getId());
        List<BookResponse> bookResponses = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponses,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BookResponse> findAllBooksByOwner(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAll(withOwnerId(user.getId()), pageable);
        List<BookResponse> bookResponses = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponses,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks = transactionHistoryRepository.findAllBorrowedBooks(pageable, user.getId());
        List<BorrowedBookResponse> bookResponses = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponses,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks = transactionHistoryRepository.findAllReturnedBooks(pageable, user.getId());
        List<BorrowedBookResponse> bookResponses = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponses,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public Integer updateShareableStatus(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the ID : " + bookId));
        User user = ((User) connectedUser.getPrincipal());
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot update others book shareable status");
        }
        book.setShareable(!book.isShareable());
        bookRepository.save(book);
        return bookId;
    }

    public Integer updateArchivedStatus(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the ID : " + bookId));
        User user = ((User) connectedUser.getPrincipal());
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot update others book archived status");
        }
        book.setArchived(!book.isArchived());
        bookRepository.save(book);
        return bookId;
    }

    public Integer borrowBook(Integer bookId, Authentication connectedUser) {
        Book book = getBookOrThrow(bookId);

        validateBorrowable(book);

        User user = getUser(connectedUser);

        if (Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot borrow your own book");
        }

        final boolean isAlreadyBorrowed = bookTransactionHistoryRepository.isAlreadyBorrowedByUser(bookId, user.getId());

        if (isAlreadyBorrowed) {
            throw new OperationNotPermittedException("The requested book is already borrowed");
        }

        BookTransactionHistory bookTransactionHistory =
                BookTransactionHistory.builder()
                        .user(user)
                        .book(book)
                        .returned(false)
                        .returnedApproved(false)
                        .build();

        var saved = transactionHistoryRepository.save(bookTransactionHistory);

        notificationService.sendNotification(
                book.getOwner(),
                BORROWED,
                "Your book has been borrowed",
                book.getTitle());

        log.info("Book {} creted by {} has been borrowed", book.getTitle(), book.getCreatedBy());
        return saved.getId();
    }

    public Integer returnBorrowBook(Integer bookId, Authentication connectedUser) {
        Book book = getBookOrThrow(bookId);

        validateBorrowable(book);

        User user = getUser(connectedUser);

        if (Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot borrow your own book");
        }

        BookTransactionHistory bookTransactionHistory = transactionHistoryRepository
                .findByBookIdAndUserId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("You did not borrow this book"));

        bookTransactionHistory.setReturned(true);

        var saved = transactionHistoryRepository.save(bookTransactionHistory);

        notificationService.sendNotification(
                book.getOwner(),
                RETURNED,
                "Your book has been returned",
                book.getTitle());

        return saved.getId();
    }

    public Integer approveReturnBorrowBook(Integer bookId, Authentication connectedUser) {
        Book book = getBookOrThrow(bookId);

        validateBorrowable(book);

        User user = getUser(connectedUser);

        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot return a book that you do do not own");
        }

        BookTransactionHistory bookTransactionHistory = transactionHistoryRepository
                .findByBookIdAndOwnerId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("You book is not returned yet. You cannot approve its return"));

        bookTransactionHistory.setReturnedApproved(true);

        var saved =  transactionHistoryRepository.save(bookTransactionHistory);

        notificationService.sendNotification(
                bookTransactionHistory.getUser(),
                RETURN_APPROVED,
               "You book return has been approved",
                book.getTitle());

        return saved.getId();
    }

    public void uploadCover(MultipartFile file, Authentication connectedUser, Integer bookId) {
        if (file == null || file.isEmpty()) return;

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the ID : " + bookId));
        User user = ((User) connectedUser.getPrincipal());
        var bookCover = fileStorageService.saveFile(file, user.getId());
        log.info(bookCover);
        book.setBookCover(bookCover);
        bookRepository.save(book);
    }

    @Transactional
    public void addReservation(Integer bookId, Authentication connectedUser) {
        Book book = getBookOrThrow(bookId);

        if (book.isArchived()) {
            throw new OperationNotPermittedException("Archived book cannot be reserved");
        }

        User user = getUser(connectedUser);

        if (Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot reserve a book that you do own");
        }

        if (reservationRepository.existsByBookAndUser(book, user)) {
            throw new OperationNotPermittedException("You already reserved this book");
        }

        boolean isBorrowed = transactionHistoryRepository.existsByBookIdAndReturnedFalse(bookId);
        if (!isBorrowed) {
            throw new OperationNotPermittedException("This book is currently available, you can borrow it directly");
        }

        BookReservation reservation = BookReservation.builder()
                .book(book)
                .user(user)
                .build();

        reservationRepository.save(reservation);

        notificationService.sendNotification(
                book.getOwner(),
                RESERVED,
                user.getUsername() + " has reserved your book",
                book.getTitle());
    }

    @Transactional
    public void removeReservation(Integer bookId, Authentication connectedUser) {
        User user = getUser(connectedUser);

        BookReservation reservation = reservationRepository.findByBookIdAndUserId(bookId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("No reservation found for this user and book"));

        reservationRepository.delete(reservation);

        Book book = reservation.getBook();

        notificationService.sendNotification(
                book.getOwner(),
                CANCELLED,
                user.getUsername() + " has cancelled the reservation for your book",
                book.getTitle()
        );
    }

    public PageResponse<BookResponse> getUserReservations(int page, int size, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<BookReservation> reservationsPage =
                reservationRepository.findAllByUserId(user.getId(), pageable);

        List<BookResponse> responses = reservationsPage.stream()
                .map(bookMapper::toReservationBookResponse)
                .toList();

        return new PageResponse<>(
                responses,
                reservationsPage.getNumber(),
                reservationsPage.getSize(),
                reservationsPage.getTotalElements(),
                reservationsPage.getTotalPages(),
                reservationsPage.isFirst(),
                reservationsPage.isLast()
        );
    }

    private Book getBookOrThrow(Integer bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the ID: " + bookId));
    }

    private void validateBorrowable(Book book) {
        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("This book cannot be borrowed (archived or not shareable).");
        }
    }

    private User getUser(Authentication auth) {
        return (User) auth.getPrincipal();
    }
}
