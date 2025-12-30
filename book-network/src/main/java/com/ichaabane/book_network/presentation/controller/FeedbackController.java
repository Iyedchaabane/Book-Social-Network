package com.ichaabane.book_network.presentation.controller;

import com.ichaabane.book_network.application.dto.request.FeedbackRequest;
import com.ichaabane.book_network.application.dto.response.FeedbackResponse;
import com.ichaabane.book_network.application.dto.response.PageResponse;
import com.ichaabane.book_network.application.service.FeedbackService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("feedbacks")
@RequiredArgsConstructor
@Tag(name = "FeedBack")
public class FeedbackController {

    private final FeedbackService service;

    @PostMapping
    public ResponseEntity<Integer> saveFeedback(
            @Valid @RequestBody FeedbackRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(service.saveFeedback(request, connectedUser));
    }

    @GetMapping("/books/{book-id}")
    public ResponseEntity<PageResponse<FeedbackResponse>> findAllFeedbackByBook(
            @PathVariable("book-id") Integer bookId,
            @RequestParam(name = "page", defaultValue = "10", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(service.findAllFeedbackByBook(bookId, page, size, connectedUser));
    }
}
