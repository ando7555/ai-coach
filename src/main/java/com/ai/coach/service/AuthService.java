package com.ai.coach.service;

import com.ai.coach.domain.entity.EmailConfirmationToken;
import com.ai.coach.domain.entity.User;
import com.ai.coach.domain.entity.UserRole;
import com.ai.coach.domain.repository.EmailConfirmationTokenRepository;
import com.ai.coach.domain.repository.UserRepository;
import com.ai.coach.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final EmailConfirmationTokenRepository confirmationTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final GoogleIdentityService googleIdentityService;
    private final PasswordEncoder passwordEncoder;
    private final EmailConfirmationNotifier emailConfirmationNotifier;
    private final Set<String> adminEmails;
    private final long emailTokenExpirationMinutes;

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern BASIC_EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public AuthService(UserRepository userRepository,
                       EmailConfirmationTokenRepository confirmationTokenRepository,
                       JwtTokenProvider tokenProvider,
                       GoogleIdentityService googleIdentityService,
                       PasswordEncoder passwordEncoder,
                       EmailConfirmationNotifier emailConfirmationNotifier,
                       @Value("${pitchmind.auth.admin-emails:}") String adminEmails,
                       @Value("${pitchmind.auth.email-token-expiration-minutes:60}") long emailTokenExpirationMinutes) {
        this.userRepository = userRepository;
        this.confirmationTokenRepository = confirmationTokenRepository;
        this.tokenProvider = tokenProvider;
        this.googleIdentityService = googleIdentityService;
        this.passwordEncoder = passwordEncoder;
        this.emailConfirmationNotifier = emailConfirmationNotifier;
        this.adminEmails = parseAdminEmails(adminEmails);
        this.emailTokenExpirationMinutes = emailTokenExpirationMinutes;
    }

    @Transactional
    public AuthPayload authenticateWithGoogle(String idToken) {
        GoogleIdentityService.GoogleProfile profile = googleIdentityService.verify(idToken);
        String normalizedEmail = normalizeEmail(profile.email());
        UserRole role = roleForEmail(normalizedEmail);

        User user = userRepository.findByGoogleSubject(profile.subject())
                .or(() -> userRepository.findByEmail(normalizedEmail))
                .orElseGet(User::new);

        user.setGoogleSubject(profile.subject());
        user.setEmail(normalizedEmail);
        user.setUsername(normalizedEmail);
        user.setDisplayName(StringUtils.hasText(profile.displayName()) ? profile.displayName() : normalizedEmail);
        user.setPictureUrl(profile.pictureUrl());
        user.setEmailConfirmed(true);
        user.setRole(role);

        user = userRepository.save(user);
        log.info("Google user authenticated: {} with role {}", normalizedEmail, role);
        return issueToken(user);
    }

    @Transactional
    public EmailRegistrationResult registerWithEmail(EmailRegistrationInput input) {
        if (input == null) {
            throw new IllegalArgumentException("Registration input is required");
        }
        String email = normalizeEmail(input.email());
        validateEmail(email);
        validatePassword(input.password());
        emailConfirmationNotifier.requireDeliveryAvailable();

        String displayName = StringUtils.hasText(input.displayName()) ? input.displayName().trim() : email;
        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null && StringUtils.hasText(existingUser.getPasswordHash())) {
            throw new IllegalArgumentException("Email is already registered. Sign in instead.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusMinutes(emailTokenExpirationMinutes);
        String confirmationToken = UUID.randomUUID().toString();

        confirmationTokenRepository.findByEmailAndConsumedAtIsNull(email).forEach(existingToken -> {
            existingToken.setConsumedAt(now);
            confirmationTokenRepository.save(existingToken);
        });

        EmailConfirmationToken token = EmailConfirmationToken.builder()
                .token(confirmationToken)
                .email(email)
                .displayName(displayName)
                .passwordHash(passwordEncoder.encode(input.password()))
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();
        confirmationTokenRepository.save(token);

        emailConfirmationNotifier.sendConfirmation(email, confirmationToken);
        return new EmailRegistrationResult(
                email,
                expiresAt,
                "Check your email to confirm your PitchMind account before signing in."
        );
    }

    @Transactional
    public AuthPayload confirmEmail(String tokenValue) {
        if (!StringUtils.hasText(tokenValue)) {
            throw new IllegalArgumentException("Confirmation token is required");
        }

        EmailConfirmationToken token = confirmationTokenRepository.findByToken(tokenValue.trim())
                .orElseThrow(() -> new IllegalArgumentException("Confirmation token is invalid"));
        if (token.getConsumedAt() != null) {
            throw new IllegalArgumentException("Confirmation token was already used");
        }
        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Confirmation token has expired");
        }

        User user = userRepository.findByEmail(token.getEmail()).orElseGet(User::new);
        user.setEmail(token.getEmail());
        user.setUsername(token.getEmail());
        user.setDisplayName(StringUtils.hasText(token.getDisplayName()) ? token.getDisplayName() : token.getEmail());
        user.setPasswordHash(token.getPasswordHash());
        user.setEmailConfirmed(true);
        user.setRole(roleForEmail(token.getEmail()));

        User savedUser = userRepository.save(user);
        token.setConsumedAt(OffsetDateTime.now());
        confirmationTokenRepository.save(token);

        log.info("Email confirmed for {} with role {}", savedUser.getEmail(), savedUser.getRole());
        return issueToken(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthPayload authenticateWithEmail(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Password is required");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!StringUtils.hasText(user.getPasswordHash()) || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        if (!user.isEmailConfirmed()) {
            throw new IllegalArgumentException("Confirm your email before signing in");
        }

        return issueToken(user);
    }

    private AuthPayload issueToken(User user) {
        String token = tokenProvider.generateToken(user.getEmail(), user.getRole().name());
        return new AuthPayload(token, user);
    }

    private UserRole roleForEmail(String normalizedEmail) {
        return adminEmails.contains(normalizedEmail) ? UserRole.ADMIN : UserRole.COACH;
    }

    private static void validateEmail(String email) {
        if (!StringUtils.hasText(email) || !BASIC_EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("A valid email is required");
        }
    }

    private static void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least %d characters".formatted(MIN_PASSWORD_LENGTH));
        }
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

    public record EmailRegistrationInput(String email, String displayName, String password) {}
    public record EmailRegistrationResult(String email, OffsetDateTime expiresAt, String message) {}
    public record AuthPayload(String token, User user) {}
}
