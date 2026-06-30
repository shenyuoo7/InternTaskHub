package com.internhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileRequest(
        @NotBlank @Size(max = 80) String displayName,
        @Size(max = 100) String password) {
}
