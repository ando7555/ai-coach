package com.ai.coach.service;

import com.ai.coach.domain.entity.User;
import com.ai.coach.domain.entity.UserRole;
import com.ai.coach.domain.repository.UserRepository;
import com.ai.coach.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock JwtTokenProvider tokenProvider;
    @Mock GoogleIdentityService googleIdentityService;

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

        AuthService service = new AuthService(userRepository, tokenProvider, googleIdentityService, "andokhachatryan986@gmail.com");

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

        AuthService service = new AuthService(userRepository, tokenProvider, googleIdentityService, "andokhachatryan986@gmail.com");

        AuthService.AuthPayload payload = service.authenticateWithGoogle("google-token");

        assertThat(payload.user().getRole()).isEqualTo(UserRole.COACH);
    }
}
