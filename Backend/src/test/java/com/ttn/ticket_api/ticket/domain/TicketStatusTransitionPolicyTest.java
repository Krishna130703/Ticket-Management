package com.ttn.ticket_api.ticket.domain;

import com.ttn.ticket_api.common.exception.InvalidStatusTransitionException;
import com.ttn.ticket_api.ticket.entity.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketStatusTransitionPolicyTest {

    private TicketStatusTransitionPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new TicketStatusTransitionPolicy();
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"IN_PROGRESS", "CANCELLED"}, mode = EnumSource.Mode.INCLUDE)
    void validateTransition_fromOpen_allowsValidTargets(TicketStatus target) {
        assertDoesNotThrow(() -> policy.validateTransition(TicketStatus.OPEN, target));
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"RESOLVED", "CANCELLED"}, mode = EnumSource.Mode.INCLUDE)
    void validateTransition_fromInProgress_allowsValidTargets(TicketStatus target) {
        assertDoesNotThrow(() -> policy.validateTransition(TicketStatus.IN_PROGRESS, target));
    }

    @Test
    void validateTransition_fromResolved_allowsClosed() {
        assertDoesNotThrow(() -> policy.validateTransition(TicketStatus.RESOLVED, TicketStatus.CLOSED));
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"OPEN", "RESOLVED", "CLOSED"})
    void validateTransition_fromOpen_rejectsInvalidTargets(TicketStatus target) {
        assertThrows(InvalidStatusTransitionException.class,
                () -> policy.validateTransition(TicketStatus.OPEN, target));
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"OPEN", "IN_PROGRESS", "CLOSED"})
    void validateTransition_fromInProgress_rejectsInvalidTargets(TicketStatus target) {
        assertThrows(InvalidStatusTransitionException.class,
                () -> policy.validateTransition(TicketStatus.IN_PROGRESS, target));
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"OPEN", "IN_PROGRESS", "RESOLVED", "CANCELLED"})
    void validateTransition_fromResolved_rejectsInvalidTargets(TicketStatus target) {
        assertThrows(InvalidStatusTransitionException.class,
                () -> policy.validateTransition(TicketStatus.RESOLVED, target));
    }

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void validateTransition_fromClosed_rejectsAllTargets(TicketStatus target) {
        assertThrows(InvalidStatusTransitionException.class,
                () -> policy.validateTransition(TicketStatus.CLOSED, target));
    }

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void validateTransition_fromCancelled_rejectsAllTargets(TicketStatus target) {
        assertThrows(InvalidStatusTransitionException.class,
                () -> policy.validateTransition(TicketStatus.CANCELLED, target));
    }
}
