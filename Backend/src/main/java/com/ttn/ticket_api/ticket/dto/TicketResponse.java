package com.ttn.ticket_api.ticket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ttn.ticket_api.comment.dto.CommentResponse;
import com.ttn.ticket_api.ticket.entity.TicketPriority;
import com.ttn.ticket_api.ticket.entity.TicketStatus;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketPriority priority,
        String assignee,
        TicketStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> comments
) {
}
