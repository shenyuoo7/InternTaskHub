package com.internhub.controller;

import com.internhub.dto.StatusUpdateRequest;
import com.internhub.dto.TaskRequest;
import com.internhub.dto.TaskResponse;
import com.internhub.enums.TaskStatus;
import com.internhub.service.TaskService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> listTasks(
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore) {
        return taskService.listTasks(currentUserId, status, assigneeId, keyword, dueBefore);
    }

    @GetMapping("/{id}")
    public TaskResponse getTask(
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @PathVariable Long id) {
        return taskService.getTask(currentUserId, id);
    }

    @PostMapping
    public TaskResponse createTask(
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @Valid @RequestBody TaskRequest request) {
        return taskService.createTask(currentUserId, request);
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        return taskService.updateTask(currentUserId, id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return taskService.updateStatus(currentUserId, id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @PathVariable Long id) {
        taskService.deleteTask(currentUserId, id);
        return ResponseEntity.noContent().build();
    }
}
