package com.internhub.controller;

import com.internhub.dto.UserDto;
import com.internhub.dto.UserProfileRequest;
import com.internhub.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> listUsers() {
        return userService.listUsers();
    }

    @PutMapping("/me")
    public UserDto updateProfile(
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @Valid @RequestBody UserProfileRequest request) {
        return userService.updateProfile(currentUserId, request);
    }
}
