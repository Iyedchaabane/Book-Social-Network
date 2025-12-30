package com.ichaabane.book_network.domain.model;

import com.ichaabane.book_network.domain.model.common.BaseEntity;
import com.ichaabane.book_network.domain.model.Feedback;
import com.ichaabane.book_network.domain.model.BookTransactionHistory;
import com.ichaabane.book_network.domain.model.BookReservation;
import com.ichaabane.book_network.domain.model.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Book extends BaseEntity {

    private String title;
    private String authorName;
    private String isbn;
    private String synopsis;
    private String bookCover;
    private boolean archived;
    private boolean shareable;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "book")
    private List<Feedback> feedbacks;

    @OneToMany(mappedBy = "book")
    private List<BookTransactionHistory> histories;

    @OneToMany(mappedBy = "book")
    private List<BookReservation> reservation;

    @Transient
    public double getRate() {
        if (feedbacks == null || feedbacks.isEmpty()) {
            return 0.0;
        }
        var rate = this.feedbacks.stream()
                .mapToDouble(Feedback::getNote)
                .average()
                .orElse(0.0);

        return Math.round(rate * 10.0) / 10.0;
    }
}
