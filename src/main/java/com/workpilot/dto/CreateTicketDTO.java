package com.workpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTicketDTO(
        @NotNull Long categoryId,
        @NotBlank
        @Size(max = 160) String title,
        @NotBlank
        @Size(max = 10000) String description,
        @NotBlank
        @Pattern(regexp = "P1|P2|P3") String priority) {
}
