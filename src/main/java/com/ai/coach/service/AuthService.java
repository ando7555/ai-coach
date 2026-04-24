package com.ai.coach.service;

import com.ai.coach.domain.EnumParser;
import com.ai.coach.domain.entity.User;
import com.ai.coach.domain.entity.UserRole;
import com.ai.coach.domain.repository.UserRepository;
import com.ai.coach.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthPayload register(String username, String password, String role) {
        log.debug("Registering user: {}", username);
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken: " + username);
        }

        UserRole assignedRole = EnumParser.parse(UserRole.class, role, UserRole.COACH);

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(assignedRole)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {} with role {}", username, assignedRole);
        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name());
        return new AuthPayload(token, user);
    }

    @Transactional(readOnly = true)
    public AuthPayload login(String username, String password) {
        log.debug("Login attempt for user: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name());
        return new AuthPayload(token, user);
    }

    public record AuthPayload(String token, User user) {}
}
