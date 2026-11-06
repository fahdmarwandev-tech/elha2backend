package com.game3alhaq.service;

import com.game3alhaq.config.CustomUserDetails;
import com.game3alhaq.config.JwtService;
import com.game3alhaq.dto.request.AuthRequests;
import com.game3alhaq.dto.response.AuthResponses;
import com.game3alhaq.entity.RefreshToken;
import com.game3alhaq.entity.User;
import com.game3alhaq.entity.UserOtp;
import com.game3alhaq.repository.RefreshTokenRepository;
import com.game3alhaq.repository.UserOtpRepository;
import com.game3alhaq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserOtpRepository userOtpRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.access-token.expiration:900000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration-days:14}")
    private int refreshTokenExpirationDays;

    public AuthResponses.AuthTokenResponse register(AuthRequests.RegisterRequest request) {
        log.info("Processing registration for phone: {}", request.getPhone());
        validateRegistrationUniqueness(request);

        User user = buildNewUser(request);
        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = createRefreshToken(savedUser);

        return buildAuthTokenResponse(savedUser, accessToken, refreshToken.getToken());
    }

    public AuthResponses.AuthTokenResponse login(AuthRequests.LoginRequest request) {
        log.info("Processing login for identifier: {}", request.getIdentifier());
        authenticateUser(request.getIdentifier().trim(), request.getPassword());

        User user = findUserByIdentifier(request.getIdentifier().trim());
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        refreshTokenRepository.deleteByUser(user);
        RefreshToken refreshToken = createRefreshToken(user);
        String accessToken = jwtService.generateToken(new CustomUserDetails(user));

        return buildAuthTokenResponse(user, accessToken, refreshToken.getToken());
    }

    public AuthResponses.AuthTokenResponse refresh(AuthRequests.RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));

        if (Boolean.TRUE.equals(refreshToken.getIsRevoked()) || refreshToken.getExpiryDate().isBefore(OffsetDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }

        refreshToken.setExpiryDate(OffsetDateTime.now().plusDays(refreshTokenExpirationDays));
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);

        return buildAuthTokenResponse(user, accessToken, refreshToken.getToken());
    }

    public void logout(AuthRequests.RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            refreshTokenRepository.findByToken(request.getRefreshToken())
                    .ifPresent(refreshTokenRepository::delete);
        }
    }

    public AuthResponses.MessageResponse forgotPassword(AuthRequests.ForgotPasswordRequest request) {
        String trimmed = request.getIdentifier().trim();
        log.info("Processing forgot-password for identifier: {}", trimmed);

        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        Optional<User> userOpt = userRepository.findByPhone(trimmed)
                .or(() -> userRepository.findByEmail(trimmed));

        UserOtp otp = UserOtp.builder()
                .user(userOpt.orElse(null))
                .identifier(trimmed)
                .otpCode(otpCode)
                .purpose("PASSWORD_RESET")
                .expiryDate(OffsetDateTime.now().plusMinutes(15))
                .isUsed(false)
                .build();
        userOtpRepository.save(otp);

        dispatchResetOtpEmail(trimmed, userOpt.orElse(null), otpCode);
        return new AuthResponses.MessageResponse("تم إرسال رمز التحقق بنجاح", true);
    }

    public AuthResponses.MessageResponse resetPassword(AuthRequests.ResetPasswordRequest request) {
        String trimmed = request.getIdentifier().trim();
        log.info("Processing reset-password for identifier: {}", trimmed);

        UserOtp otp = userOtpRepository
                .findTopByIdentifierAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(trimmed, "PASSWORD_RESET")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "رمز التحقق غير صالح أو مستخدم"));

        validateOtp(otp, request.getOtpCode().trim());
        updateUserPassword(otp, trimmed, request.getNewPassword());

        otp.setIsUsed(true);
        userOtpRepository.save(otp);
        return new AuthResponses.MessageResponse("تم تغيير كلمة المرور بنجاح", true);
    }

    private void validateRegistrationUniqueness(AuthRequests.RegisterRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already registered");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
    }

    private User buildNewUser(AuthRequests.RegisterRequest req) {
        return User.builder()
                .fullName(req.getFullName().trim())
                .phone(req.getPhone().trim())
                .email(req.getEmail() != null ? req.getEmail().trim() : null)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role("ROLE_USER")
                .isActive(true)
                .build();
    }

    private void authenticateUser(String identifier, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, password));
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid phone/email or password");
        }
    }

    private User findUserByIdentifier(String identifier) {
        return userRepository.findByPhone(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void dispatchResetOtpEmail(String identifier, User user, String otpCode) {
        String targetEmail = null;
        if (identifier.contains("@")) {
            targetEmail = identifier;
        } else if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
            targetEmail = user.getEmail();
        }
        if (targetEmail != null) {
            mailService.sendPasswordResetOtp(targetEmail, otpCode);
        }
    }

    private void validateOtp(UserOtp otp, String enteredCode) {
        if (otp.getExpiryDate().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "انتهت صلاحية رمز التحقق");
        }
        if (!otp.getOtpCode().equals(enteredCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "رمز التحقق غير صحيح");
        }
    }

    private void updateUserPassword(UserOtp otp, String identifier, String newPassword) {
        User user = (otp.getUser() != null) ? otp.getUser() : userRepository.findByPhone(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "المستخدم غير موجود"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(OffsetDateTime.now().plusDays(refreshTokenExpirationDays))
                .isRevoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    private AuthResponses.AuthTokenResponse buildAuthTokenResponse(User user, String accessToken, String refreshToken) {
        AuthResponses.UserResponse userResponse = AuthResponses.UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();

        return AuthResponses.AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .user(userResponse)
                .build();
    }
}
