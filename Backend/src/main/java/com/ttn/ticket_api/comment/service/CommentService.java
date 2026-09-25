package com.ttn.ticket_api.comment.service;

import com.ttn.ticket_api.comment.dto.AddCommentRequest;
import com.ttn.ticket_api.comment.dto.CommentResponse;
import com.ttn.ticket_api.comment.entity.Comment;
import com.ttn.ticket_api.comment.repository.CommentRepository;
import com.ttn.ticket_api.common.exception.TicketNotFoundException;
import com.ttn.ticket_api.ticket.entity.Ticket;
import com.ttn.ticket_api.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;

    public CommentService(TicketRepository ticketRepository, CommentRepository commentRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public CommentResponse addComment(Long ticketId, AddCommentRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setBody(request.body());

        Comment saved = commentRepository.save(comment);
        return new CommentResponse(
                saved.getId(),
                ticket.getId(),
                saved.getBody(),
                saved.getCreatedAt()
        );
    }
}
