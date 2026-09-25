package com.ttn.ticket_api.ticket.dto.validation;

import com.ttn.ticket_api.ticket.dto.UpdateTicketRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UpdateTicketRequestValidator implements ConstraintValidator<ValidUpdateTicketRequest, UpdateTicketRequest> {

    @Override
    public boolean isValid(UpdateTicketRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return false;
        }

        context.disableDefaultConstraintViolation();
        boolean valid = true;

        boolean hasField = request.getTitle() != null
                || request.getDescription() != null
                || request.getPriority() != null
                || request.isAssigneeSpecified();

        if (!hasField) {
            context.buildConstraintViolationWithTemplate("At least one field must be provided")
                    .addConstraintViolation();
            valid = false;
        }

        if (request.getTitle() != null && request.getTitle().isBlank()) {
            context.buildConstraintViolationWithTemplate("must not be blank")
                    .addPropertyNode("title")
                    .addConstraintViolation();
            valid = false;
        }

        if (request.getDescription() != null && request.getDescription().isBlank()) {
            context.buildConstraintViolationWithTemplate("must not be blank")
                    .addPropertyNode("description")
                    .addConstraintViolation();
            valid = false;
        }

        if (request.isAssigneeSpecified() && request.getAssignee() != null && request.getAssignee().isBlank()) {
            context.buildConstraintViolationWithTemplate("must not be blank")
                    .addPropertyNode("assignee")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
