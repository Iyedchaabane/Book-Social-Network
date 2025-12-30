package com.ichaabane.book_network.domain.repository;

import com.ichaabane.book_network.domain.model.BookTransactionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BookTransactionHistoryRepository extends JpaRepository<BookTransactionHistory, Integer> {
    @Query("""
            SELECT history
            FROM BookTransactionHistory history
            where history.user.id = :userId
            """)
    Page<BookTransactionHistory> findAllBorrowedBooks(Pageable pageable, Integer userId);

    @Query("""
            SELECT history
            FROM BookTransactionHistory history
            where history.book.owner.id = :userId
            """)
    Page<BookTransactionHistory> findAllReturnedBooks(Pageable pageable, Integer userId);

    @Query("""
            SELECT
            (COUNT(*) > 0) AS isBorrowed
            FROM BookTransactionHistory history
            where history.user.id = :userId
            AND history.book.id = :bookId
            AND history.returnedApproved = false
            """)
    boolean isAlreadyBorrowedByUser(Integer bookId, Integer userId);

    @Query("""
            SELECT history
            FROM BookTransactionHistory history
            WHERE history.user.id = :userId
            AND history.book.id = :bookId
            AND history.returned = false
            AND history.returnedApproved = false
            """)
    Optional<BookTransactionHistory> findByBookIdAndUserId(Integer bookId, Integer userId);

    @Query("""
            SELECT history
            FROM BookTransactionHistory history
            where history.book.owner.id = :userId
            AND history.book.id = :bookId
            AND history.returned = true
            AND history.returnedApproved = false
            """)
    Optional<BookTransactionHistory> findByBookIdAndOwnerId(Integer bookId, Integer userId);

    boolean existsByBookIdAndReturnedFalse(Integer bookId);
}
