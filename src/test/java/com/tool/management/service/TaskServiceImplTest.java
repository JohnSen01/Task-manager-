package com.tool.management.service;

import com.tool.management.domain.Task;
import com.tool.management.domain.TaskPriority;
import com.tool.management.domain.TaskStatus;
import com.tool.management.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskServiceImpl Unit Tests")
class TaskServiceImplTest {

    @Mock
    private TaskRepository repository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task sampleTask;

    @BeforeEach
    void setUp() {
        sampleTask = Task.builder()
                .id(1L)
                .title("Write unit tests")
                .description("Cover all service methods")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // createTask 

    @Test
    @DisplayName("createTask: saves and returns task with TODO status")
    void createTask_validRequest_returnsSavedTask() {
        CreateTaskRequest request = new CreateTaskRequest("Write unit tests", "details", TaskPriority.HIGH, null);
        when(repository.save(any(Task.class))).thenReturn(sampleTask);

        Task result = taskService.createTask(request);

        assertNotNull(result);
        assertEquals("Write unit tests", result.getTitle());
        assertEquals(TaskStatus.TODO, result.getStatus());
        verify(repository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("createTask: blank title throws IllegalArgumentException")
    void createTask_blankTitle_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new CreateTaskRequest("  ", null, TaskPriority.LOW, null));
    }

    // updateTask

    @Test
    @DisplayName("updateTask: valid status transition TODO -> IN_PROGRESS")
    void updateTask_validTransition_updatesStatus() {
        UpdateTaskRequest request = new UpdateTaskRequest(null, null, TaskStatus.IN_PROGRESS, null, null);
        Task updatedTask = sampleTask.toBuilder().status(TaskStatus.IN_PROGRESS).build();
        when(repository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(repository.save(any(Task.class))).thenReturn(updatedTask);

        Task result = taskService.updateTask(1L, request);

        assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    @DisplayName("updateTask: invalid status transition TODO -> DONE throws exception")
    void updateTask_invalidTransition_throwsException() {
        UpdateTaskRequest request = new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(sampleTask));

        assertThrows(InvalidTaskStateException.class,
                () -> taskService.updateTask(1L, request));
    }

    @Test
    @DisplayName("updateTask: non-existent task throws TaskNotFoundException")
    void updateTask_notFound_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        UpdateTaskRequest request = new UpdateTaskRequest(null, null, null, null, null);

        assertThrows(TaskNotFoundException.class, () -> taskService.updateTask(99L, request));
    }

    // getTaskById 

    @Test
    @DisplayName("getTaskById: returns task when found")
    void getTaskById_exists_returnsTask() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleTask));
        Optional<Task> result = taskService.getTaskById(1L);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    @DisplayName("getTaskById: returns empty when not found")
    void getTaskById_notFound_returnsEmpty() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertTrue(taskService.getTaskById(99L).isEmpty());
    }

    // getAllTasks 

    @Test
    @DisplayName("getAllTasks: filters by status")
    void getAllTasks_withStatusFilter_callsFilteredQuery() {
        when(repository.findByStatus(TaskStatus.TODO)).thenReturn(List.of(sampleTask));
        List<Task> result = taskService.getAllTasks(TaskStatus.TODO, null);
        assertEquals(1, result.size());
        verify(repository).findByStatus(TaskStatus.TODO);
    }

    @Test
    @DisplayName("getAllTasks: no filter returns all")
    void getAllTasks_noFilter_returnsAll() {
        when(repository.findAll()).thenReturn(List.of(sampleTask));
        List<Task> result = taskService.getAllTasks(null, null);
        assertEquals(1, result.size());
        verify(repository).findAll();
    }

    @Test
    @DisplayName("getAllTasks: filters by status and priority together")
    void getAllTasks_withStatusAndPriority_callsCombinedQuery() {
        when(repository.findByStatusAndPriority(TaskStatus.TODO, TaskPriority.HIGH))
                .thenReturn(List.of(sampleTask));
        List<Task> result = taskService.getAllTasks(TaskStatus.TODO, TaskPriority.HIGH);
        assertEquals(1, result.size());
        verify(repository).findByStatusAndPriority(TaskStatus.TODO, TaskPriority.HIGH);
        verify(repository, never()).findByStatus(any());
        verify(repository, never()).findByPriority(any());
    }

    // deleteTask 

    @Test
    @DisplayName("deleteTask: calls repository delete when task exists")
    void deleteTask_exists_deletesSuccessfully() {
        when(repository.deleteById(1L)).thenReturn(true);
        taskService.deleteTask(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteTask: throws TaskNotFoundException when not found")
    void deleteTask_notFound_throwsException() {
        when(repository.deleteById(99L)).thenReturn(false);
        assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(99L));
        verify(repository).deleteById(99L);
    }
}
