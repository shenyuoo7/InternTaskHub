package com.internhub.service;

import com.internhub.dto.UserDto;
import com.internhub.dto.UserProfileRequest;
import com.internhub.entity.UserAccount;
import com.internhub.repository.UserAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserAccountRepository userRepository;
    private final CurrentUserService currentUserService;

    public UserService(UserAccountRepository userRepository, CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<UserDto> listUsers() {
        return userRepository.findAll().stream()
                .map(UserDto::from)
                .toList();
    }

    @Transactional
    public UserDto updateProfile(Long currentUserId, UserProfileRequest request) {
        UserAccount user = currentUserService.resolve(currentUserId);
        user.setDisplayName(request.displayName().trim());
        if (StringUtils.hasText(request.password())) {
            user.setPassword(request.password().trim());
        }
        return UserDto.from(userRepository.save(user));
    }
}
