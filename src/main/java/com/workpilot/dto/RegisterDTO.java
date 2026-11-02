package com.workpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterDTO(
        @NotBlank
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{2,63}") String loginName,
        @NotBlank
        @Size(min = 8, max = 72) String password,
        @NotBlank
        @Size(max = 80) String displayName) {
}
