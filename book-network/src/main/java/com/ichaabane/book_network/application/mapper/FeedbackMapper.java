package com.ichaabane.book_network.application.mapper;

import com.ichaabane.book_network.application.dto.request.FeedbackRequest;
import com.ichaabane.book_network.application.dto.response.FeedbackResponse;
import com.ichaabane.book_network.domain.model.Book;
import com.ichaabane.book_network.domain.model.Feedback;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class FeedbackMapper {

    public Feedback toFeedback(FeedbackRequest request) {
        return Feedback.builder()
                .note(request.note())
                .comment(request.comment())
                .book(Book.builder()
                        .id(request.bookId())
                        .build())
                .build();
    }

    public FeedbackResponse toFeedbackResponse(Feedback f, Integer userId) {
        return FeedbackResponse.builder()
                .note(f.getNote())
                .comment(f.getComment())
                .ownFeedback(Objects.equals(f.getCreatedBy(), userId))
                .build();
    }
}
