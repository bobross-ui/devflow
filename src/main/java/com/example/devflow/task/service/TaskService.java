package com.example.devflow.task.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.devflow.exception.EntityNotFoundException;
import com.example.devflow.session.entity.CodingSession;
import com.example.devflow.session.repository.CodingSessionRepository;
import com.example.devflow.task.entity.Task;
import com.example.devflow.task.repository.TaskRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final CodingSessionRepository sessionRepository;

    private CodingSession validateSessionOwnership(Long sessionId, Long userId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found or not owned by user"));
    }

    public Task createTask(Long sessionId, Long userId, String description) {
        CodingSession session = validateSessionOwnership(sessionId, userId);

        Task task = Task.builder()
                .session(session)
                .description(description)
                .completed(false)
                .build();

        return taskRepository.save(task);
    }

    // Get all tasks for a session
    public Page<Task> getTasksBySession(Long sessionId, Long userId, Pageable pageable) {
        validateSessionOwnership(sessionId, userId);
        return taskRepository.findBySessionId(sessionId, pageable);
    }

    // Get a specific task (with ownership check)
    public Task getTaskByIdAndSession(Long taskId, Long sessionId, Long userId) {
        validateSessionOwnership(sessionId, userId);

        return taskRepository.findByIdAndSessionId(taskId, sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found in this session"));
    }

    public Task updateTask(Long taskId, Long sessionId, Long userId, String description, Boolean completed) {
        Task task = getTaskByIdAndSession(taskId, sessionId, userId);

        if (description != null) {
            task.setDescription(description);
        }
        if (completed != null) {
            task.setCompleted(completed);
        }

        return taskRepository.save(task);
    }

    // Delete a task
    public void deleteTask(Long taskId, Long sessionId, Long userId) {
        Task task = getTaskByIdAndSession(taskId, sessionId, userId);
        taskRepository.delete(task);
    }
}
