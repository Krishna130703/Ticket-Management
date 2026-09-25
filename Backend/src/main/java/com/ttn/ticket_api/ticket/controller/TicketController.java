package com.ttn.ticket_api.ticket.controller;

import com.ttn.ticket_api.ticket.dto.ChangeStatusRequest;
import com.ttn.ticket_api.ticket.dto.CreateTicketRequest;
import com.ttn.ticket_api.ticket.dto.TicketListResponse;
import com.ttn.ticket_api.ticket.dto.TicketResponse;
import com.ttn.ticket_api.ticket.dto.UpdateTicketRequest;
import com.ttn.ticket_api.ticket.entity.TicketStatus;
import com.ttn.ticket_api.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = ticketService.createTicket(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/tickets/" + response.id())
                .body(response);
    }

    @GetMapping
    public TicketListResponse listTickets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TicketStatus status) {
        if (keyword != null && keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("keyword must not be blank");
        }
        return ticketService.listTickets(keyword, status);
    }

    @GetMapping("/{id}")
    public TicketResponse getTicket(@PathVariable Long id) {
        return ticketService.getTicket(id);
    }

    @PatchMapping("/{id}")
    public TicketResponse updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request) {
        return ticketService.updateTicket(id, request);
    }

    @PostMapping("/{id}/status")
    public TicketResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request) {
        return ticketService.changeStatus(id, request);
    }
}
