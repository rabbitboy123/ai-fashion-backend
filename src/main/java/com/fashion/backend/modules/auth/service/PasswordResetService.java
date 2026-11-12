package com.fashion.backend.modules.auth.service;

import com.fashion.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.fashion.backend.modules.auth.dto.request.ResetPasswordRequest;
import com.fashion.backend.modules.auth.entity.PasswordResetCode;
import com.fashion.backend.modules.auth.repository.PasswordResetCodeRepository;
import com.fashion.backend.modules.user.entity.User;
import com.fashion.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository resetCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.password-reset.code-expiration-minutes:10}")
    private long expirationMinutes;

    @Value("${app.password-reset.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Value("${app.password-reset.max-attempts:5}")
    private int maxAttempts;

    @Transactional
    public void requestCode(ForgotPasswordRequest request) {
        String normalizedEmail = normalize(request.email());
        userRepository.findByEmailForUpdate(normalizedEmail).ifPresent(user -> issueCode(user));
    }

    private void issueCode(User user) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetCode record = resetCodeRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> PasswordResetCode.builder().user(user).build());

        if (record.getLastSentAt() != null
                && record.getLastSentAt().plusSeconds(resendCooldownSeconds).isAfter(now)) {
            return;
        }

        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        record.setCodeHash(passwordEncoder.encode(code));
        record.setExpiresAt(now.plusMinutes(expirationMinutes));
        record.setLastSentAt(now);
        record.setFailedAttempts(0);
        resetCodeRepository.save(record);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(user.getEmail());
            message.setSubject("Mã đặt lại mật khẩu");
            message.setText("Mã xác minh của bạn là " + code + ". Mã có hiệu lực trong "
                    + expirationMinutes + " phút. Nếu bạn không yêu cầu, hãy bỏ qua email này.");
            mailSender.send(message);
        } catch (MailException ex) {
            record.setExpiresAt(now);
            // Do not lock the user out of retrying if the SMTP server is temporarily unavailable.
            record.setLastSentAt(now.minusSeconds(resendCooldownSeconds));
            resetCodeRepository.save(record);
            log.warn("Password reset email delivery failed for user id {}: {}",
                    user.getId(), ex.getClass().getSimpleName());
        }
    }

    @Transactional
    public boolean resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = normalize(request.email());
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            return false;
        }

        PasswordResetCode record = resetCodeRepository.findByUserIdForUpdate(user.getId()).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (record == null || !record.getExpiresAt().isAfter(now)
                || record.getFailedAttempts() >= maxAttempts) {
            return false;
        }

        if (!passwordEncoder.matches(request.code(), record.getCodeHash())) {
            record.setFailedAttempts(record.getFailedAttempts() + 1);
            if (record.getFailedAttempts() >= maxAttempts) {
                record.setExpiresAt(now);
            }
            resetCodeRepository.save(record);
            return false;
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        resetCodeRepository.delete(record);
        return true;
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
