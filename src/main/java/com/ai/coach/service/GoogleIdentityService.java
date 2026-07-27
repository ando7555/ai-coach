package com.ai.coach.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
public class GoogleIdentityService {

    private final String googleClientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleIdentityService(@Value("${pitchmind.auth.google-client-id:}") String googleClientId) {
        this.googleClientId = googleClientId == null ? "" : googleClientId.trim();
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(StringUtils.hasText(this.googleClientId) ? List.of(this.googleClientId) : List.of())
                .build();
    }

    public GoogleProfile verify(String idToken) {
        if (!StringUtils.hasText(googleClientId)) {
            throw new IllegalStateException("Google sign-in is not configured");
        }

        if (!StringUtils.hasText(idToken)) {
            throw new IllegalArgumentException("Google credential is required");
        }

        GoogleIdToken verifiedToken;
        try {
            verifiedToken = verifier.verify(idToken);
        } catch (GeneralSecurityException | IOException ex) {
            throw new IllegalArgumentException("Could not verify Google credential", ex);
        }

        if (verifiedToken == null) {
            throw new IllegalArgumentException("Invalid Google credential");
        }

        GoogleIdToken.Payload payload = verifiedToken.getPayload();
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new IllegalArgumentException("Google account email must be verified");
        }

        String email = payload.getEmail();
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Google credential did not include an email address");
        }

        return new GoogleProfile(
                payload.getSubject(),
                email,
                stringClaim(payload, "name", email),
                stringClaim(payload, "picture", null)
        );
    }

    private static String stringClaim(GoogleIdToken.Payload payload, String key, String fallback) {
        Object value = payload.get(key);
        return value instanceof String text && StringUtils.hasText(text) ? text : fallback;
    }

    public record GoogleProfile(String subject, String email, String displayName, String pictureUrl) {}
}
