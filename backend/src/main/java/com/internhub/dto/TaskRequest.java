package com.internhub.dto;

import com.internhub.enums.Priority;
import com.internhub.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record TaskRequest(
        @NotBlank String title,
        String description,
        TaskStatus status,
        Priority priority,
        Long assigneeId,
        LocalDate dueDate) {
}
