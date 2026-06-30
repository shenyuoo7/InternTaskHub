package com.internhub.service;

import com.internhub.dto.AuthResponse;
import com.internhub.dto.LoginRequest;
import com.internhub.dto.UserDto;
import com.internhub.entity.UserAccount;
import com.internhub.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserAccountRepository userRepository;

    public AuthService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthResponse mockLogin(LoginRequest request) {
        UserAccount user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("账号或密码错误"));
        if (!user.getPassword().equals(request.password())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        String token = "mock-token-" + user.getId() + "-" + user.getRole();
        return new AuthResponse(token, UserDto.from(user));
    }
}
