package com.internhub.dto;

import com.internhub.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull TaskStatus status) {
}
