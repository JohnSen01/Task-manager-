package com.tool.management.service;

import com.tool.management.domain.TaskPriority;
import com.tool.management.domain.TaskStatus;

import java.time.LocalDateTime;

/**
 * Immutable request DTO for updating a task.
 * Null fields mean "no change" (partial update).
 */
public record UpdateTaskRequest(
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime dueDate
) {
    public UpdateTaskRequest {
        if (title != null && title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
    }
}
