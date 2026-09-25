package com.ttn.ticket_api.ticket.service;

import com.ttn.ticket_api.common.exception.InvalidStatusTransitionException;
import com.ttn.ticket_api.common.exception.TicketNotFoundException;
import com.ttn.ticket_api.ticket.domain.TicketStatusTransitionPolicy;
import com.ttn.ticket_api.ticket.dto.ChangeStatusRequest;
import com.ttn.ticket_api.ticket.dto.CreateTicketRequest;
import com.ttn.ticket_api.ticket.dto.TicketResponse;
import com.ttn.ticket_api.ticket.entity.Ticket;
import com.ttn.ticket_api.ticket.entity.TicketPriority;
import com.ttn.ticket_api.ticket.entity.TicketStatus;
import com.ttn.ticket_api.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Spy
    private TicketStatusTransitionPolicy transitionPolicy = new TicketStatusTransitionPolicy();

    @InjectMocks
    private TicketService ticketService;

    @Test
    void createTicket_setsStatusOpen() {
        CreateTicketRequest request = new CreateTicketRequest("Title", "Description", TicketPriority.HIGH, null);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(1L);
            return ticket;
        });

        TicketResponse response = ticketService.createTicket(request);

        assertEquals(TicketStatus.OPEN, response.status());
    }

    @Test
    void changeStatus_whenInvalidTransition_throwsAndDoesNotSave() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setTitle("Title");
        ticket.setDescription("Description");
        ticket.setPriority(TicketPriority.LOW);

        when(ticketRepository.findByIdWithComments(1L)).thenReturn(Optional.of(ticket));

        assertThrows(InvalidStatusTransitionException.class,
                () -> ticketService.changeStatus(1L, new ChangeStatusRequest(TicketStatus.OPEN)));

        verify(ticketRepository, never()).save(any(Ticket.class));
        assertEquals(TicketStatus.CLOSED, ticket.getStatus());
    }

    @Test
    void getTicket_whenMissing_throwsNotFound() {
        when(ticketRepository.findByIdWithComments(99L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketService.getTicket(99L));
    }
}
