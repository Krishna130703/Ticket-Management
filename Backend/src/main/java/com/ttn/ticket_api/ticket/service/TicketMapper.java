package com.ttn.ticket_api.ticket.service;

import com.ttn.ticket_api.comment.dto.CommentResponse;
import com.ttn.ticket_api.comment.entity.Comment;
import com.ttn.ticket_api.ticket.dto.TicketResponse;
import com.ttn.ticket_api.ticket.entity.Ticket;

import java.util.Comparator;
import java.util.List;

final class TicketMapper {

    private TicketMapper() {
    }

    static TicketResponse toResponse(Ticket ticket, boolean includeComments) {
        List<CommentResponse> comments = null;
        if (includeComments) {
            comments = ticket.getComments().stream()
                    .sorted(Comparator.comparing(Comment::getCreatedAt))
                    .map(TicketMapper::toCommentResponse)
                    .toList();
        }

        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                comments
        );
    }

    static CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                comment.getBody(),
                comment.getCreatedAt()
        );
    }
}
