package com.ichaabane.book_network.domain.model;

import com.ichaabane.book_network.domain.model.Book;
import com.ichaabane.book_network.domain.model.common.BaseEntity;
import com.ichaabane.book_network.domain.model.User;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BookReservation extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}