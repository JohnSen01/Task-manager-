package com.tool.management.domain;

/**
 * Represents the lifecycle status of a Task.
 * Enforces valid transitions: TODO -> IN_PROGRESS -> DONE | CANCELLED
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    CANCELLED;

    public boolean canTransitionTo(TaskStatus next) {
        return switch (this) {
            case TODO -> next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS -> next == DONE || next == CANCELLED;
            case DONE, CANCELLED -> false;
        };
    }
}
