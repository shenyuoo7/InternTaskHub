package com.internhub.service;

import com.internhub.dto.UserDto;
import com.internhub.repository.UserAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserAccountRepository userRepository;

    public UserService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserDto> listUsers() {
        return userRepository.findAll().stream()
                .map(UserDto::from)
                .toList();
    }
}
