package com.ttn.ticket_api.ticket.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.ttn.ticket_api.ticket.dto.validation.ValidUpdateTicketRequest;
import com.ttn.ticket_api.ticket.entity.TicketPriority;
import lombok.Getter;
import lombok.Setter;

@Getter
@ValidUpdateTicketRequest
public class UpdateTicketRequest {

    @Setter
    private String title;

    @Setter
    private String description;

    @Setter
    private TicketPriority priority;

    private String assignee;

    @JsonIgnore
    private boolean assigneeSpecified;

    @JsonSetter("assignee")
    public void setAssigneeValue(String assignee) {
        this.assignee = assignee;
        this.assigneeSpecified = true;
    }
}
