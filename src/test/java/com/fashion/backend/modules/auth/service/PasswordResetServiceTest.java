package com.fashion.backend.modules.auth.service;

import com.fashion.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.fashion.backend.modules.auth.dto.request.ResetPasswordRequest;
import com.fashion.backend.modules.auth.entity.PasswordResetCode;
import com.fashion.backend.modules.auth.repository.PasswordResetCodeRepository;
import com.fashion.backend.modules.user.entity.User;
import com.fashion.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetCodeRepository resetCodeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JavaMailSender mailSender;

    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, resetCodeRepository, passwordEncoder, mailSender);
        ReflectionTestUtils.setField(service, "fromAddress", "no-reply@example.test");
        ReflectionTestUtils.setField(service, "expirationMinutes", 10L);
        ReflectionTestUtils.setField(service, "resendCooldownSeconds", 60L);
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        user = User.builder().id(7L).email("user@example.test").password("old").build();
    }

    @Test
    void requestCodeSendsSixDigitCodeAndStoresOnlyEncodedValue() {
        when(userRepository.findByEmailForUpdate("user@example.test")).thenReturn(Optional.of(user));
        when(resetCodeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-code");

        service.requestCode(new ForgotPasswordRequest(" USER@example.test "));

        ArgumentCaptor<PasswordResetCode> codeCaptor = ArgumentCaptor.forClass(PasswordResetCode.class);
        verify(resetCodeRepository).save(codeCaptor.capture());
        assertEquals("encoded-code", codeCaptor.getValue().getCodeHash());
        assertTrue(codeCaptor.getValue().getExpiresAt().isAfter(LocalDateTime.now()));

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        String body = mailCaptor.getValue().getText();
        assertTrue(Pattern.compile("\\b\\d{6}\\b").matcher(body).find());
    }

    @Test
    void requestCodeUsesCooldown() {
        when(userRepository.findByEmailForUpdate("user@example.test")).thenReturn(Optional.of(user));
        PasswordResetCode current = PasswordResetCode.builder()
                .user(user).codeHash("hashed").lastSentAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusMinutes(10))
                .failedAttempts(0).build();
        when(resetCodeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(current));

        service.requestCode(new ForgotPasswordRequest("user@example.test"));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(resetCodeRepository, never()).save(any(PasswordResetCode.class));
    }

    @Test
    void wrongCodeIncrementsAttemptCount() {
        when(userRepository.findByEmail("user@example.test")).thenReturn(Optional.of(user));
        PasswordResetCode current = PasswordResetCode.builder()
                .user(user).codeHash("hashed").lastSentAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusMinutes(5)).failedAttempts(0).build();
        when(resetCodeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(current));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(false);

        boolean result = service.resetPassword(new ResetPasswordRequest("user@example.test", "123456", "newPass1"));

        assertFalse(result);
        assertEquals(1, current.getFailedAttempts());
        verify(resetCodeRepository).save(current);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void correctCodeChangesPasswordAndConsumesCode() {
        when(userRepository.findByEmail("user@example.test")).thenReturn(Optional.of(user));
        PasswordResetCode current = PasswordResetCode.builder()
                .user(user).codeHash("hashed").lastSentAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusMinutes(5)).failedAttempts(0).build();
        when(resetCodeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(current));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("newPass1")).thenReturn("encoded-new");

        assertTrue(service.resetPassword(new ResetPasswordRequest("user@example.test", "123456", "newPass1")));

        assertEquals("encoded-new", user.getPassword());
        verify(userRepository).save(user);
        verify(resetCodeRepository).delete(current);
    }
}
