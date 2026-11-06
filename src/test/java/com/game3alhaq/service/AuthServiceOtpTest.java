package com.game3alhaq.service;

import com.game3alhaq.config.JwtService;
import com.game3alhaq.dto.request.AuthRequests.ForgotPasswordRequest;
import com.game3alhaq.dto.request.AuthRequests.ResetPasswordRequest;
import com.game3alhaq.dto.response.AuthResponses.MessageResponse;
import com.game3alhaq.entity.User;
import com.game3alhaq.entity.UserOtp;
import com.game3alhaq.repository.RefreshTokenRepository;
import com.game3alhaq.repository.UserOtpRepository;
import com.game3alhaq.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceOtpTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserOtpRepository userOtpRepository;

    @Mock
    private MailService mailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private UserOtp sampleOtp;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .fullName("أحمد الراشدي")
                .phone("+96891234567")
                .email("ahmed@example.com")
                .passwordHash("oldHashedPassword")
                .isActive(true)
                .build();

        sampleOtp = UserOtp.builder()
                .id(10L)
                .user(sampleUser)
                .identifier("+96891234567")
                .otpCode("123456")
                .purpose("PASSWORD_RESET")
                .expiryDate(OffsetDateTime.now().plusMinutes(15))
                .isUsed(false)
                .build();
    }

    @Test
    @DisplayName("Should generate OTP and send email when identifier is registered user")
    void testForgotPasswordSuccess() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .identifier("+96891234567")
                .build();

        when(userRepository.findByPhone("+96891234567")).thenReturn(Optional.of(sampleUser));
        when(userOtpRepository.save(any(UserOtp.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = authService.forgotPassword(request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        verify(userOtpRepository, times(1)).save(any(UserOtp.class));
        verify(mailService, times(1)).sendPasswordResetOtp(eq("ahmed@example.com"), any(String.class));
    }

    @Test
    @DisplayName("Should reset password successfully when OTP is valid and unexpired")
    void testResetPasswordSuccess() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .identifier("+96891234567")
                .otpCode("123456")
                .newPassword("newSecurePass123")
                .build();

        when(userOtpRepository.findTopByIdentifierAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(
                "+96891234567", "PASSWORD_RESET")).thenReturn(Optional.of(sampleOtp));
        when(passwordEncoder.encode("newSecurePass123")).thenReturn("newHashedPassword");

        MessageResponse response = authService.resetPassword(request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("newHashedPassword", sampleUser.getPasswordHash());
        assertTrue(sampleOtp.getIsUsed());
        verify(userRepository, times(1)).save(sampleUser);
        verify(userOtpRepository, times(1)).save(sampleOtp);
    }

    @Test
    @DisplayName("Should throw ResponseStatusException when OTP code is incorrect")
    void testResetPasswordInvalidOtp() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .identifier("+96891234567")
                .otpCode("999999")
                .newPassword("newSecurePass123")
                .build();

        when(userOtpRepository.findTopByIdentifierAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(
                "+96891234567", "PASSWORD_RESET")).thenReturn(Optional.of(sampleOtp));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword(request));
        assertTrue(ex.getReason().contains("رمز التحقق غير صحيح"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResponseStatusException when OTP is expired")
    void testResetPasswordExpiredOtp() {
        sampleOtp.setExpiryDate(OffsetDateTime.now().minusMinutes(1));
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .identifier("+96891234567")
                .otpCode("123456")
                .newPassword("newSecurePass123")
                .build();

        when(userOtpRepository.findTopByIdentifierAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(
                "+96891234567", "PASSWORD_RESET")).thenReturn(Optional.of(sampleOtp));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword(request));
        assertTrue(ex.getReason().contains("انتهت صلاحية رمز التحقق"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResponseStatusException when no unused OTP is found")
    void testResetPasswordNoOtpFound() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .identifier("+96891234567")
                .otpCode("123456")
                .newPassword("newSecurePass123")
                .build();

        when(userOtpRepository.findTopByIdentifierAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(
                "+96891234567", "PASSWORD_RESET")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> authService.resetPassword(request));
    }
}
