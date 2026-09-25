package com.ttn.ticket_api.ticket.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = UpdateTicketRequestValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUpdateTicketRequest {

    String message() default "Invalid update request";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
