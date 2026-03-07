package com.ai.coach.controller;

import com.ai.coach.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class AuthGraphQLController {

    private final AuthService authService;

    @MutationMapping
    public AuthService.AuthPayload register(@Argument String username,
                                               @Argument String password,
                                               @Argument String role) {
        return authService.register(username, password, role);
    }

    @MutationMapping
    public AuthService.AuthPayload login(@Argument String username, @Argument String password) {
        return authService.login(username, password);
    }
}
