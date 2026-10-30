package com.devrenanrodrigues.goalmanagementapi.auth;

import com.devrenanrodrigues.goalmanagementapi.auth.dto.AuthResponse;
import com.devrenanrodrigues.goalmanagementapi.auth.dto.GoogleLoginRequest;
import com.devrenanrodrigues.goalmanagementapi.auth.dto.GoogleUserInfo;
import com.devrenanrodrigues.goalmanagementapi.auth.dto.LoginRequest;
import com.devrenanrodrigues.goalmanagementapi.auth.dto.RefreshTokenRequest;
import com.devrenanrodrigues.goalmanagementapi.auth.dto.RegisterRequest;
import com.devrenanrodrigues.goalmanagementapi.common.exception.BusinessException;
import com.devrenanrodrigues.goalmanagementapi.common.exception.UnauthorizedException;
import com.devrenanrodrigues.goalmanagementapi.common.security.JwtTokenService;
import com.devrenanrodrigues.goalmanagementapi.user.UserEntity;
import com.devrenanrodrigues.goalmanagementapi.user.UserRepository;
import com.devrenanrodrigues.goalmanagementapi.user.dto.UserResponse;
import com.devrenanrodrigues.goalmanagementapi.user.enums.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    @Value("${security.jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new BusinessException("Já existe um usuário cadastrado com este e-mail.");
        }

        UserEntity user = UserEntity.builder()
                .name(request.name().trim())
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .provider(AuthProvider.LOCAL)
                .build();

        UserEntity savedUser = userRepository.save(user);
        return issueTokensForUser(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BusinessException("E-mail ou senha inválidos."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("E-mail ou senha inválidos.");
        }

        return issueTokensForUser(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserInfo googleUser = googleTokenVerifierService.verifyToken(request.token().trim());
        String normalizedEmail = googleUser.email().trim().toLowerCase();

        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .map(existingUser -> updateExistingGoogleUser(existingUser, googleUser))
                .orElseGet(() -> createGoogleUser(googleUser, normalizedEmail));

        return issueTokensForUser(user);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshTokenEntity storedToken = refreshTokenRepository.findByTokenWithUser(request.refreshToken().trim())
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida ou expirada. Faça login novamente."));

        if (storedToken.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(storedToken.getUser().getId());
            throw new UnauthorizedException("Sessão inválida ou expirada. Faça login novamente.");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new UnauthorizedException("Sessão expirada. Faça login novamente.");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return issueTokensForUser(storedToken.getUser());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByTokenWithUser(request.refreshToken().trim())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private AuthResponse issueTokensForUser(UserEntity user) {
        String accessToken = jwtTokenService.generateToken(user.getId(), user.getEmail());
        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.of(accessToken, refreshToken.getToken(), UserResponse.fromEntity(user));
    }

    private UserEntity updateExistingGoogleUser(UserEntity existingUser, GoogleUserInfo googleUser) {
        boolean updated = false;

        if (existingUser.getProviderId() == null && googleUser.sub() != null) {
            existingUser.setProviderId(googleUser.sub());
            updated = true;
        }

        if (googleUser.picture() != null && !googleUser.picture().isBlank() && !googleUser.picture().equals(existingUser.getAvatarUrl())) {
            existingUser.setAvatarUrl(googleUser.picture());
            updated = true;
        }

        return updated ? userRepository.save(existingUser) : existingUser;
    }

    private UserEntity createGoogleUser(GoogleUserInfo googleUser, String normalizedEmail) {
        String displayName = (googleUser.name() != null && !googleUser.name().isBlank())
                ? googleUser.name().trim()
                : normalizedEmail.split("@")[0];

        UserEntity newUser = UserEntity.builder()
                .name(displayName)
                .email(normalizedEmail)
                .avatarUrl(googleUser.picture())
                .provider(AuthProvider.GOOGLE)
                .providerId(googleUser.sub())
                .build();

        return userRepository.save(newUser);
    }
}
