package com.ttn.ticket_api.common.exception;

import com.ttn.ticket_api.ticket.entity.TicketStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(TicketStatus from, TicketStatus to) {
        super("Transition from " + from + " to " + to + " is not allowed.");
    }
}
