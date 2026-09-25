package com.ttn.ticket_api.ticket.service;

import com.ttn.ticket_api.common.exception.TicketNotFoundException;
import com.ttn.ticket_api.ticket.domain.TicketStatusTransitionPolicy;
import com.ttn.ticket_api.ticket.dto.ChangeStatusRequest;
import com.ttn.ticket_api.ticket.dto.CreateTicketRequest;
import com.ttn.ticket_api.ticket.dto.TicketListResponse;
import com.ttn.ticket_api.ticket.dto.TicketResponse;
import com.ttn.ticket_api.ticket.dto.UpdateTicketRequest;
import com.ttn.ticket_api.ticket.entity.Ticket;
import com.ttn.ticket_api.ticket.entity.TicketStatus;
import com.ttn.ticket_api.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketStatusTransitionPolicy transitionPolicy;

    public TicketService(TicketRepository ticketRepository, TicketStatusTransitionPolicy transitionPolicy) {
        this.ticketRepository = ticketRepository;
        this.transitionPolicy = transitionPolicy;
    }

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setPriority(request.priority());
        ticket.setAssignee(request.assignee());
        ticket.setStatus(TicketStatus.OPEN);

        Ticket saved = ticketRepository.save(ticket);
        return TicketMapper.toResponse(saved, true);
    }

    @Transactional(readOnly = true)
    public TicketListResponse listTickets(String keyword, TicketStatus status) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        List<Ticket> tickets = ticketRepository.findByKeywordAndStatus(normalizedKeyword, status);
        List<TicketResponse> summaries = tickets.stream()
                .map(ticket -> TicketMapper.toResponse(ticket, false))
                .toList();
        return new TicketListResponse(summaries);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findByIdWithComments(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        return TicketMapper.toResponse(ticket, true);
    }

    @Transactional
    public TicketResponse updateTicket(Long ticketId, UpdateTicketRequest request) {
        Ticket ticket = ticketRepository.findByIdWithComments(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (request.getTitle() != null) {
            ticket.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            ticket.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        if (request.isAssigneeSpecified()) {
            ticket.setAssignee(request.getAssignee());
        }

        Ticket saved = ticketRepository.save(ticket);
        return TicketMapper.toResponse(saved, true);
    }

    @Transactional
    public TicketResponse changeStatus(Long ticketId, ChangeStatusRequest request) {
        Ticket ticket = ticketRepository.findByIdWithComments(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        transitionPolicy.validateTransition(ticket.getStatus(), request.status());
        ticket.setStatus(request.status());

        Ticket saved = ticketRepository.save(ticket);
        return TicketMapper.toResponse(saved, true);
    }
}
