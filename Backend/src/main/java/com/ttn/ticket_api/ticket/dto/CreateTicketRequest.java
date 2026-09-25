package com.ttn.ticket_api.ticket.dto;

import com.ttn.ticket_api.ticket.dto.validation.NotBlankIfPresent;
import com.ttn.ticket_api.ticket.entity.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotNull TicketPriority priority,
        @NotBlankIfPresent String assignee
) {
}
