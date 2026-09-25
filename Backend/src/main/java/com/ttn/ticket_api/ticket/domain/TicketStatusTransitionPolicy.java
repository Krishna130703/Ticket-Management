package com.ttn.ticket_api.ticket.domain;

import com.ttn.ticket_api.common.exception.InvalidStatusTransitionException;
import com.ttn.ticket_api.ticket.entity.TicketStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class TicketStatusTransitionPolicy {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = Map.of(
            TicketStatus.OPEN, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
            TicketStatus.IN_PROGRESS, Set.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED),
            TicketStatus.RESOLVED, Set.of(TicketStatus.CLOSED),
            TicketStatus.CLOSED, Set.of(),
            TicketStatus.CANCELLED, Set.of()
    );

    public void validateTransition(TicketStatus from, TicketStatus to) {
        Set<TicketStatus> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowedTargets.contains(to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }

    public boolean isAllowed(TicketStatus from, TicketStatus to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
