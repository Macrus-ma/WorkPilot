package com.workpilot.vo;

import java.time.LocalDateTime;

public record LoginVO(String accessToken, LocalDateTime expiresAt) {
}
