package com.ai.coach.service;

import com.ai.coach.domain.entity.EmailConfirmationToken;
import com.ai.coach.domain.entity.User;
import com.ai.coach.domain.entity.UserRole;
import com.ai.coach.domain.repository.EmailConfirmationTokenRepository;
import com.ai.coach.domain.repository.UserRepository;
import com.ai.coach.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock EmailConfirmationTokenRepository confirmationTokenRepository;
    @Mock JwtTokenProvider tokenProvider;
    @Mock GoogleIdentityService googleIdentityService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailConfirmationNotifier emailConfirmationNotifier;

    @Test
    void assignsAdminOnlyForAllowListedGoogleEmail() {
        when(googleIdentityService.verify("google-token"))
                .thenReturn(new GoogleIdentityService.GoogleProfile(
                        "google-subject",
                        "AndoKhachatryan986@gmail.com",
                        "Ando Khachatryan",
                        "https://example.com/avatar.png"
                ));
        when(userRepository.findByGoogleSubject("google-subject")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("andokhachatryan986@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProvider.generateToken("andokhachatryan986@gmail.com", "ADMIN")).thenReturn("app-jwt");

        AuthService service = authService("andokhachatryan986@gmail.com");

        AuthService.AuthPayload payload = service.authenticateWithGoogle("google-token");

        assertThat(payload.token()).isEqualTo("app-jwt");
        assertThat(payload.user().getEmail()).isEqualTo("andokhachatryan986@gmail.com");
        assertThat(payload.user().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void assignsCoachForEveryOtherGoogleEmail() {
        when(googleIdentityService.verify("google-token"))
                .thenReturn(new GoogleIdentityService.GoogleProfile(
                        "google-subject",
                        "analyst@example.com",
                        "Analyst",
                        null
                ));
        when(userRepository.findByGoogleSubject("google-subject")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("analyst@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProvider.generateToken("analyst@example.com", "COACH")).thenReturn("app-jwt");

        AuthService service = authService("andokhachatryan986@gmail.com");

        AuthService.AuthPayload payload = service.authenticateWithGoogle("google-token");

        assertThat(payload.user().getRole()).isEqualTo(UserRole.COACH);
    }

    @Test
    void registersEmailAccountByCreatingPendingConfirmationToken() {
        when(userRepository.findByEmail("coach@example.com")).thenReturn(Optional.empty());
        when(confirmationTokenRepository.findByEmailAndConsumedAtIsNull("coach@example.com")).thenReturn(java.util.List.of());
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-password");
        when(confirmationTokenRepository.save(any(EmailConfirmationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailConfirmationNotifier.sendConfirmation(eq("coach@example.com"), any())).thenReturn("http://localhost:8080/?confirmEmail=token");

        AuthService service = authService("andokhachatryan986@gmail.com");

        AuthService.EmailRegistrationResult result = service.registerWithEmail(
                new AuthService.EmailRegistrationInput("Coach@Example.com", "Coach", "secret123"));

        assertThat(result.email()).isEqualTo("coach@example.com");
        assertThat(result.message()).contains("Check your email");
    }

    @Test
    void doesNotCreatePendingTokenWhenEmailDeliveryIsUnavailable() {
        doThrow(new IllegalArgumentException("Email delivery is not configured"))
                .when(emailConfirmationNotifier).requireDeliveryAvailable();

        AuthService service = authService("andokhachatryan986@gmail.com");

        assertThatThrownBy(() -> service.registerWithEmail(
                new AuthService.EmailRegistrationInput("coach@example.com", "Coach", "secret123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email delivery");
        verify(confirmationTokenRepository, never()).save(any(EmailConfirmationToken.class));
    }


    @Test
    void confirmsEmailAndAssignsAdminOnlyForAllowListedEmail() {
        EmailConfirmationToken confirmationToken = EmailConfirmationToken.builder()
                .token("confirmation-token")
                .email("andokhachatryan986@gmail.com")
                .displayName("Ando")
                .passwordHash("hashed-password")
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();
        when(confirmationTokenRepository.findByToken("confirmation-token")).thenReturn(Optional.of(confirmationToken));
        when(userRepository.findByEmail("andokhachatryan986@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(confirmationTokenRepository.save(any(EmailConfirmationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProvider.generateToken("andokhachatryan986@gmail.com", "ADMIN")).thenReturn("app-jwt");

        AuthService service = authService("andokhachatryan986@gmail.com");

        AuthService.AuthPayload payload = service.confirmEmail("confirmation-token");

        assertThat(payload.token()).isEqualTo("app-jwt");
        assertThat(payload.user().isEmailConfirmed()).isTrue();
        assertThat(payload.user().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(payload.user().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(confirmationToken.getConsumedAt()).isNotNull();
    }

    @Test
    void blocksEmailLoginUntilEmailIsConfirmed() {
        User user = User.builder()
                .email("coach@example.com")
                .username("coach@example.com")
                .passwordHash("hashed-password")
                .emailConfirmed(false)
                .role(UserRole.COACH)
                .build();
        when(userRepository.findByEmail("coach@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed-password")).thenReturn(true);

        AuthService service = authService("andokhachatryan986@gmail.com");

        assertThatThrownBy(() -> service.authenticateWithEmail("coach@example.com", "secret123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Confirm your email");
    }

    private AuthService authService(String adminEmails) {
        return new AuthService(
                userRepository,
                confirmationTokenRepository,
                tokenProvider,
                googleIdentityService,
                passwordEncoder,
                emailConfirmationNotifier,
                adminEmails,
                60
        );
    }
}
