package com.ai.coach.service;

import com.ai.coach.domain.entity.User;
import com.ai.coach.domain.entity.UserRole;
import com.ai.coach.domain.repository.UserRepository;
import com.ai.coach.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final GoogleIdentityService googleIdentityService;
    private final Set<String> adminEmails;

    public AuthService(UserRepository userRepository,
                       JwtTokenProvider tokenProvider,
                       GoogleIdentityService googleIdentityService,
                       @Value("${pitchmind.auth.admin-emails:}") String adminEmails) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.googleIdentityService = googleIdentityService;
        this.adminEmails = parseAdminEmails(adminEmails);
    }

    @Transactional
    public AuthPayload authenticateWithGoogle(String idToken) {
        GoogleIdentityService.GoogleProfile profile = googleIdentityService.verify(idToken);
        String normalizedEmail = normalizeEmail(profile.email());
        UserRole role = adminEmails.contains(normalizedEmail) ? UserRole.ADMIN : UserRole.COACH;

        User user = userRepository.findByGoogleSubject(profile.subject())
                .or(() -> userRepository.findByEmail(normalizedEmail))
                .orElseGet(User::new);

        user.setGoogleSubject(profile.subject());
        user.setEmail(normalizedEmail);
        user.setUsername(normalizedEmail);
        user.setDisplayName(StringUtils.hasText(profile.displayName()) ? profile.displayName() : normalizedEmail);
        user.setPictureUrl(profile.pictureUrl());
        user.setRole(role);

        user = userRepository.save(user);
        log.info("Google user authenticated: {} with role {}", normalizedEmail, role);
        String token = tokenProvider.generateToken(user.getEmail(), user.getRole().name());
        return new AuthPayload(token, user);
    }

    private static Set<String> parseAdminEmails(String configuredEmails) {
        if (!StringUtils.hasText(configuredEmails)) {
            return Set.of();
        }

        return Arrays.stream(configuredEmails.split(","))
                .map(AuthService::normalizeEmail)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public record AuthPayload(String token, User user) {}
}
