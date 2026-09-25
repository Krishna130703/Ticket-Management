package com.ttn.ticket_api.ticket.repository;

import com.ttn.ticket_api.ticket.entity.Ticket;
import com.ttn.ticket_api.ticket.entity.TicketPriority;
import com.ttn.ticket_api.ticket.entity.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void findByKeywordAndStatus_filtersByKeywordAndStatus() {
        Ticket openLogin = createTicket("Login issue", "Cannot log in", TicketStatus.OPEN);
        Ticket closedLogin = createTicket("Login resolved", "User can log in", TicketStatus.CLOSED);
        createTicket("Billing issue", "Invoice mismatch", TicketStatus.OPEN);

        List<Ticket> results = ticketRepository.findByKeywordAndStatus("login", TicketStatus.OPEN);

        assertEquals(1, results.size());
        assertEquals(openLogin.getId(), results.getFirst().getId());
        assertTrue(results.stream().noneMatch(ticket -> ticket.getId().equals(closedLogin.getId())));
    }

    private Ticket createTicket(String title, String description, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setStatus(status);
        return ticketRepository.save(ticket);
    }
}
