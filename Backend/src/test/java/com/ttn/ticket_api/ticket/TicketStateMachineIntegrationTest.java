package com.ttn.ticket_api.ticket;

import tools.jackson.databind.ObjectMapper;
import com.ttn.ticket_api.ticket.dto.CreateTicketRequest;
import com.ttn.ticket_api.ticket.entity.Ticket;
import com.ttn.ticket_api.ticket.entity.TicketPriority;
import com.ttn.ticket_api.ticket.entity.TicketStatus;
import com.ttn.ticket_api.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketStateMachineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void createTicket_persistsOpenStatus() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(
                "Login issue", "Cannot log in", TicketPriority.HIGH, null);

        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();

        Long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(TicketStatus.OPEN, ticket.getStatus());
    }

    @Test
    void changeStatus_whenInvalidTransition_returns409AndPreservesStatus() throws Exception {
        Ticket ticket = new Ticket();
        ticket.setTitle("Closed ticket");
        ticket.setDescription("Already closed");
        ticket.setPriority(TicketPriority.LOW);
        ticket.setStatus(TicketStatus.CLOSED);
        ticket = ticketRepository.save(ticket);

        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        Ticket reloaded = ticketRepository.findById(ticket.getId()).orElseThrow();
        assertEquals(TicketStatus.CLOSED, reloaded.getStatus());
    }

    @Test
    void changeStatus_whenValidTransition_persistsNewStatus() throws Exception {
        Ticket ticket = new Ticket();
        ticket.setTitle("Open ticket");
        ticket.setDescription("Needs work");
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);
        ticket = ticketRepository.save(ticket);

        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        Ticket reloaded = ticketRepository.findById(ticket.getId()).orElseThrow();
        assertEquals(TicketStatus.IN_PROGRESS, reloaded.getStatus());
    }
}
