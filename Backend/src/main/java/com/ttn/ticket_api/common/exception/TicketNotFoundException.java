package com.ttn.ticket_api.common.exception;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(Long ticketId) {
        super("Ticket not found.");
    }
}
