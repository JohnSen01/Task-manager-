package com.tool.management.service;

import com.tool.management.domain.TaskPriority;

import java.time.LocalDateTime;

/**
 * Immutable request DTO for creating a task.
 */
public record CreateTaskRequest(
        String title,
        String description,
        TaskPriority priority,
        LocalDateTime dueDate
) {
    public CreateTaskRequest {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        if (priority == null) priority = TaskPriority.MEDIUM;
    }
}
