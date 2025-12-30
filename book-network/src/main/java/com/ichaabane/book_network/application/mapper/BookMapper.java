package com.ichaabane.book_network.application.mapper;

import com.ichaabane.book_network.application.dto.request.BookRequest;
import com.ichaabane.book_network.application.dto.response.BookResponse;
import com.ichaabane.book_network.application.dto.response.BorrowedBookResponse;
import com.ichaabane.book_network.domain.model.Book;
import com.ichaabane.book_network.infrastructure.file.FileUtils;
import com.ichaabane.book_network.domain.model.BookTransactionHistory;
import com.ichaabane.book_network.domain.model.BookReservation;
import org.springframework.stereotype.Service;

@Service
public class BookMapper {

    public Book toBook(BookRequest request) {
        return Book.builder()
                .id(request.id())
                .title(request.title())
                .authorName(request.authorName())
                .isbn(request.isbn())
                .synopsis(request.synopsis())
                .archived(false)
                .shareable(request.shareable())
                .build();
    }

    public BookResponse toBookResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .authorName(book.getAuthorName())
                .isbn(book.getIsbn())
                .synopsis(book.getSynopsis())
                .archived(book.isArchived())
                .shareable(book.isShareable())
                .rate(book.getRate())
                .owner(book.getOwner().getFullName())
                .cover(FileUtils.readFileFromLocation(book.getBookCover()))
                .build();
    }

    public BorrowedBookResponse toBorrowedBookResponse(BookTransactionHistory history) {
        return BorrowedBookResponse.builder()
                .id(history.getBook().getId())
                .title(history.getBook().getTitle())
                .authorName(history.getBook().getAuthorName())
                .isbn(history.getBook().getIsbn())
                .rate(history.getBook().getRate())
                .returnedApproved(history.isReturnedApproved())
                .returned(history.isReturned())
                .build();
    }

    public BookResponse toReservationBookResponse(BookReservation reservation) {
        Book book = reservation.getBook();
        return toBookResponse(book);
    }
}
