package com.ai.coach.service;

import com.ai.coach.domain.entity.User;
import com.ai.coach.domain.repository.UserRepository;
import com.ai.coach.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthPayload register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken: " + username);
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role("COACH")
                .build();

        user = userRepository.save(user);
        String token = tokenProvider.generateToken(user.getUsername(), user.getRole());
        return new AuthPayload(token, user);
    }

    @Transactional(readOnly = true)
    public AuthPayload login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole());
        return new AuthPayload(token, user);
    }

    public record AuthPayload(String token, User user) {}
}
