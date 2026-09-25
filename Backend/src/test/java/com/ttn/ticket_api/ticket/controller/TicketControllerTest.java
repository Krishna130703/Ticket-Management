package com.ttn.ticket_api.ticket.controller;

import tools.jackson.databind.ObjectMapper;
import com.ttn.ticket_api.common.advice.GlobalExceptionHandler;
import com.ttn.ticket_api.common.exception.InvalidStatusTransitionException;
import com.ttn.ticket_api.common.exception.TicketNotFoundException;
import com.ttn.ticket_api.ticket.dto.CreateTicketRequest;
import com.ttn.ticket_api.ticket.dto.TicketListResponse;
import com.ttn.ticket_api.ticket.dto.TicketResponse;
import com.ttn.ticket_api.ticket.entity.TicketPriority;
import com.ttn.ticket_api.ticket.entity.TicketStatus;
import com.ttn.ticket_api.ticket.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TicketController.class)
@Import(GlobalExceptionHandler.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TicketService ticketService;

    @Test
    void createTicket_whenTitleBlank_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(
                new CreateTicketRequest("", "Description", TicketPriority.HIGH, null));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createTicket_whenValid_returns201WithOpenStatus() throws Exception {
        Instant now = Instant.parse("2026-09-25T10:00:00Z");
        TicketResponse response = new TicketResponse(
                1L, "Title", "Description", TicketPriority.HIGH, null,
                TicketStatus.OPEN, now, now, List.of());

        when(ticketService.createTicket(any(CreateTicketRequest.class))).thenReturn(response);

        String body = objectMapper.writeValueAsString(
                new CreateTicketRequest("Title", "Description", TicketPriority.HIGH, null));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void getTicket_whenNotFound_returns404() throws Exception {
        when(ticketService.getTicket(99L)).thenThrow(new TicketNotFoundException(99L));

        mockMvc.perform(get("/api/tickets/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }

    @Test
    void changeStatus_whenInvalidTransition_returns409() throws Exception {
        when(ticketService.changeStatus(eq(1L), any()))
                .thenThrow(new InvalidStatusTransitionException(TicketStatus.CLOSED, TicketStatus.OPEN));

        mockMvc.perform(post("/api/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void listTickets_whenBlankKeyword_returns400() throws Exception {
        mockMvc.perform(get("/api/tickets").param("keyword", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listTickets_whenNoMatches_returnsEmptyList() throws Exception {
        when(ticketService.listTickets(null, null)).thenReturn(new TicketListResponse(List.of()));

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets").isArray())
                .andExpect(jsonPath("$.tickets").isEmpty());
    }

    @Test
    void updateTicket_whenEmptyBody_returns400() throws Exception {
        mockMvc.perform(patch("/api/tickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
