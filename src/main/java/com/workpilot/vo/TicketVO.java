package com.workpilot.vo;

import java.time.LocalDateTime;

public record TicketVO(Long id, Long categoryId, String title, String description,
                         String priority, String status, LocalDateTime dueAt,
                         LocalDateTime createdAt) {
}
