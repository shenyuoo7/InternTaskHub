package com.internhub.dto;

public record AuthResponse(
        String token,
        UserDto user) {
}
