package com.internhub.dto;

import com.internhub.entity.UserAccount;
import com.internhub.enums.Role;

public record UserDto(
        Long id,
        String username,
        String displayName,
        Role role,
        String avatarColor) {

    public static UserDto from(UserAccount user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getAvatarColor());
    }
}
