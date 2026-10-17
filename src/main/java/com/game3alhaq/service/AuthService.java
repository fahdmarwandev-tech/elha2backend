package com.game3alhaq.service;

import com.game3alhaq.config.CustomUserDetails;
import com.game3alhaq.config.JwtService;
import com.game3alhaq.dto.request.AuthRequests;
import com.game3alhaq.dto.response.AuthResponses;
import com.game3alhaq.entity.RefreshToken;
import com.game3alhaq.entity.User;
import com.game3alhaq.repository.RefreshTokenRepository;
import com.game3alhaq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponses.AuthTokenResponse register(AuthRequests.RegisterRequest request) {
        log.info("Processing registration for phone: {}", request.getPhone());

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already registered");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank() && userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .phone(request.getPhone().trim())
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = createRefreshToken(savedUser);

        return buildAuthTokenResponse(savedUser, accessToken, refreshToken.getToken());
    }

    public AuthResponses.AuthTokenResponse login(AuthRequests.LoginRequest request) {
        log.info("Processing login for identifier: {}", request.getIdentifier());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getIdentifier().trim(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid phone/email or password");
        }

        User user = userRepository.findByPhone(request.getIdentifier().trim())
                .or(() -> userRepository.findByEmail(request.getIdentifier().trim()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        refreshTokenRepository.deleteByUser(user);
        RefreshToken refreshToken = createRefreshToken(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);

        return buildAuthTokenResponse(user, accessToken, refreshToken.getToken());
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(OffsetDateTime.now().plusDays(7))
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
                .expiresIn(86400L) // 24 hours
                .user(userResponse)
                .build();
    }
}
