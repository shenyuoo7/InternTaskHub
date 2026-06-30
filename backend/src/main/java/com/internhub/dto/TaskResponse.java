package com.internhub.dto;

import com.internhub.entity.TaskItem;
import com.internhub.enums.Priority;
import com.internhub.enums.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Priority priority,
        UserDto assignee,
        UserDto creator,
        LocalDate dueDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TaskResponse from(TaskItem task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                UserDto.from(task.getAssignee()),
                UserDto.from(task.getCreator()),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
