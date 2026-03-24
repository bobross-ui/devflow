package com.example.devflow.user.service;

import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.devflow.user.entity.User;
import com.example.devflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(String email, String password, String displayName) {
        if(userRepository.findByEmail(email).isPresent()){
            throw new IllegalArgumentException("Email Already Registered");
        }

        User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .displayName(displayName)
                    .build();

        return userRepository.save(user);
                    
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
