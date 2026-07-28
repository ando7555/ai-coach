package com.ai.coach.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class EmailConfirmationNotifierTest {

    @Test
    void rejectsNonLocalConfirmationUrlWithoutSmtp() {
        EmailConfirmationNotifier notifier = notifier(
                "",
                "",
                "https://pitch-mind-j6zv.onrender.com/");

        assertThatThrownBy(notifier::requireDeliveryAvailable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email delivery is not configured");
    }

    @Test
    void allowsLocalConfirmationUrlWithoutSmtp() {
        EmailConfirmationNotifier notifier = notifier(
                "",
                "",
                "http://localhost:8080/");

        assertThatCode(notifier::requireDeliveryAvailable)
                .doesNotThrowAnyException();
    }

    @Test
    void allowsDevProfileWithoutSmtp() {
        EmailConfirmationNotifier notifier = notifier(
                "",
                "dev",
                "https://example.com/");

        assertThatCode(notifier::requireDeliveryAvailable)
                .doesNotThrowAnyException();
    }

    @Test
    void allowsPublicConfirmationUrlWithSmtp() {
        EmailConfirmationNotifier notifier = notifier(
                "smtp.example.com",
                "prod",
                "https://pitch-mind-j6zv.onrender.com/");

        assertThatCode(notifier::requireDeliveryAvailable)
                .doesNotThrowAnyException();
    }

    private EmailConfirmationNotifier notifier(String mailHost, String activeProfiles, String confirmationBaseUrl) {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> mailSenderProvider = mock(ObjectProvider.class);
        EmailConfirmationNotifier notifier = new EmailConfirmationNotifier(mailSenderProvider);
        ReflectionTestUtils.setField(notifier, "mailHost", mailHost);
        ReflectionTestUtils.setField(notifier, "activeProfiles", activeProfiles);
        ReflectionTestUtils.setField(notifier, "confirmationBaseUrl", confirmationBaseUrl);
        return notifier;
    }
}
