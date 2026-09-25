package com.ttn.ticket_api.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record AddCommentRequest(
        @NotBlank String body
) {
}
