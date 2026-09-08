package com.tool.management.repository;

import com.tool.management.domain.Task;
import com.tool.management.domain.TaskPriority;
import com.tool.management.domain.TaskStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Task persistence.
 * Decouples the service layer from the storage mechanism (JDBC / H2 / MySQL).
 */
public interface TaskRepository {

    Task save(Task task);

    Optional<Task> findById(long id);

    List<Task> findAll();

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByPriority(TaskPriority priority);

    List<Task> findByStatusAndPriority(TaskStatus status, TaskPriority priority);

    boolean deleteById(long id);
}
