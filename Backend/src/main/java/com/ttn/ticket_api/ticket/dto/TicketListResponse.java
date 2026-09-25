package com.ttn.ticket_api.ticket.dto;

import java.util.List;

public record TicketListResponse(
        List<TicketResponse> tickets
) {
}
