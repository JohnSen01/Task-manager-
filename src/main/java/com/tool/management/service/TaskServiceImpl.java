package com.tool.management.service;

import com.tool.management.domain.Task;
import com.tool.management.domain.TaskPriority;
import com.tool.management.domain.TaskStatus;
import com.tool.management.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of TaskService.
 * Enforces business rules (e.g. valid status transitions) before delegating to the repository.
 */
@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);
    private final TaskRepository repository;

    public TaskServiceImpl(TaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public Task createTask(CreateTaskRequest request) {
        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .priority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM)
                .status(TaskStatus.TODO)
                .dueDate(request.dueDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Task saved = repository.save(task);
        log.info("Created task: {}", saved);
        return saved;
    }

    @Override
    public Task updateTask(long id, UpdateTaskRequest request) {
        Task existing = repository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found: " + id));

        // Validate status transition
        if (request.status() != null && !existing.getStatus().canTransitionTo(request.status())) {
            throw new InvalidTaskStateException(
                    "Cannot transition from %s to %s".formatted(existing.getStatus(), request.status()));
        }

        Task updated = existing.toBuilder()
                .title(request.title() != null ? request.title() : existing.getTitle())
                .description(request.description() != null ? request.description() : existing.getDescription())
                .status(request.status() != null ? request.status() : existing.getStatus())
                .priority(request.priority() != null ? request.priority() : existing.getPriority())
                .dueDate(request.dueDate() != null ? request.dueDate() : existing.getDueDate())
                .updatedAt(LocalDateTime.now())
                .build();

        Task saved = repository.save(updated);
        log.info("Updated task: {}", saved);
        return saved;
    }

    @Override
    public Optional<Task> getTaskById(long id) {
        return repository.findById(id);
    }

    @Override
    public List<Task> getAllTasks(TaskStatus statusFilter, TaskPriority priorityFilter) {
        if (statusFilter != null && priorityFilter != null) {
            return repository.findByStatusAndPriority(statusFilter, priorityFilter);
        }
        if (statusFilter != null) return repository.findByStatus(statusFilter);
        if (priorityFilter != null) return repository.findByPriority(priorityFilter);
        return repository.findAll();
    }

    @Override
    public void deleteTask(long id) {
        if (!repository.deleteById(id)) {
            throw new TaskNotFoundException("Task not found: " + id);
        }
        log.info("Deleted task id={}", id);
    }
}
