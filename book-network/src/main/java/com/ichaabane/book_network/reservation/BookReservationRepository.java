package com.ichaabane.book_network.reservation;

import com.ichaabane.book_network.book.Book;
import com.ichaabane.book_network.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookReservationRepository extends JpaRepository<BookReservation, Integer> {

    boolean existsByBookAndUser(Book book, User user);

    Optional<BookReservation> findByBookIdAndUserId(Integer bookId, Integer userId);

    Page<BookReservation> findAllByUserId(Integer userId, Pageable pageable);
}
