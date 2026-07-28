package com.ai.coach.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailConfirmationNotifier {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${pitchmind.auth.confirmation-base-url}")
    private String confirmationBaseUrl;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public String sendConfirmation(String email, String token) {
        String confirmationLink = UriComponentsBuilder
                .fromUriString(confirmationBaseUrl)
                .queryParam("confirmEmail", token)
                .build()
                .toUriString();

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || !StringUtils.hasText(mailHost)) {
            log.info("Email confirmation link for {}: {}", email, confirmationLink);
            return confirmationLink;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(fromAddress)) {
            message.setFrom(fromAddress);
        }
        message.setTo(email);
        message.setSubject("Confirm your PitchMind account");
        message.setText("""
                Welcome to PitchMind.

                Confirm your account by opening this link:
                %s

                If you did not request this account, you can ignore this email.
                """.formatted(confirmationLink));
        mailSender.send(message);
        return confirmationLink;
    }
}
