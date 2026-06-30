package com.internhub.service;

import com.internhub.entity.UserAccount;
import com.internhub.exception.NotFoundException;
import com.internhub.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserAccountRepository userRepository;

    public CurrentUserService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserAccount resolve(Long currentUserId) {
        if (currentUserId == null) {
            return userRepository.findByUsername("mentor")
                    .orElseThrow(() -> new NotFoundException("Default mentor user not found"));
        }
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("Current user not found"));
    }
}
