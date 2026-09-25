package com.ttn.ticket_api.comment.controller;

import com.ttn.ticket_api.comment.dto.AddCommentRequest;
import com.ttn.ticket_api.comment.dto.CommentResponse;
import com.ttn.ticket_api.comment.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long ticketId,
            @Valid @RequestBody AddCommentRequest request) {
        CommentResponse response = commentService.addComment(ticketId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/tickets/" + ticketId + "/comments/" + response.id())
                .body(response);
    }
}
