package com.workpilot.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(@NotBlank String loginName,
                           @NotBlank String password) {
}
