package com.tool.management.service;

import com.tool.management.domain.Task;
import com.tool.management.domain.TaskPriority;
import com.tool.management.domain.TaskStatus;

import java.util.List;
import java.util.Optional;

/**
 * Business logic interface for Task operations.
 */
public interface TaskService {

    Task createTask(CreateTaskRequest request);

    Task updateTask(long id, UpdateTaskRequest request);

    Optional<Task> getTaskById(long id);

    List<Task> getAllTasks(TaskStatus statusFilter, TaskPriority priorityFilter);

    void deleteTask(long id);
}
