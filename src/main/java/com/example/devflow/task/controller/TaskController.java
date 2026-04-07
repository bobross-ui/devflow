package com.example.devflow.task.controller;

import com.example.devflow.DevflowApplication;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.devflow.auth.filter.CustomUserDetails;
import com.example.devflow.task.dto.CreateTaskRequest;
import com.example.devflow.task.dto.TaskResponse;
import com.example.devflow.task.dto.UpdateTaskRequest;
import com.example.devflow.task.entity.Task;
import com.example.devflow.task.service.TaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {
    
    private final TaskService taskService;

    
    // Helper method to convert entity to DTO
    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .description(task.getDescription())
                .completed(task.getCompleted())
                .createdAt(task.getCreatedAt())
                .build();
    }
    
    // POST /sessions/{sessionId}/tasks - Create a task
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId,
            @Valid @RequestBody CreateTaskRequest request) {
        
        Task task = taskService.createTask(sessionId, userDetails.getUserId(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(task));
    }
    
    // GET /sessions/{sessionId}/tasks - List all tasks for a session
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getTasksBySession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page,size,Sort.by("createdAt").ascending());
        
        Page<Task> tasks = taskService.getTasksBySession(sessionId, userDetails.getUserId(), pageable);
        return ResponseEntity.ok(tasks.map(this::toResponse));
    }
    
    // PATCH /sessions/{sessionId}/tasks/{taskId} - Update a task
    @PatchMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        
        Task task = taskService.updateTask(taskId, sessionId, userDetails.getUserId(), 
                request.getDescription(), request.getCompleted());
        return ResponseEntity.ok(toResponse(task));
    }
    
    // DELETE /sessions/{sessionId}/tasks/{taskId} - Delete a task
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId,
            @PathVariable Long taskId) {
        
        taskService.deleteTask(taskId, sessionId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}