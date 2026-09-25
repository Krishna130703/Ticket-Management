package com.ttn.ticket_api.ticket.dto;

import com.ttn.ticket_api.ticket.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(
        @NotNull TicketStatus status
) {
}
