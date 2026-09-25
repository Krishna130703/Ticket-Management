package com.ttn.ticket_api.comment.dto;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long ticketId,
        String body,
        Instant createdAt
) {
}
