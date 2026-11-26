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
import com.devrenanrodrigues.goalmanagementapi.user.enums.AuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private GoogleTokenVerifierService googleTokenVerifierService;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpirationDays", 7L);

        userId = UUID.randomUUID();
        userEntity = UserEntity.builder()
                .name("Jane Doe")
                .email("jane.doe@example.com")
                .passwordHash("encodedPassword")
                .provider(AuthProvider.LOCAL)
                .build();
        userEntity.setId(userId);
        userEntity.setCreatedAt(Instant.now());
    }

    @Test
    void shouldThrowBusinessExceptionWhenEmailAlreadyExistsOnRegister() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane.doe@example.com", "password123");
        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe um usuário cadastrado com este e-mail.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRegisterUserSuccessfullyWhenDataIsValid() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane.doe@example.com", "password123");
        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        when(jwtTokenService.generateToken(eq(userId), eq("jane.doe@example.com"))).thenReturn("mockedAccessToken");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("mockedAccessToken");
        assertThat(response.refreshToken()).isNotNull();
        assertThat(response.user().email()).isEqualTo("jane.doe@example.com");
        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    @Test
    void shouldThrowBusinessExceptionWhenUserNotFoundByEmailOnLogin() {
        LoginRequest request = new LoginRequest("unknown@example.com", "password123");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("E-mail ou senha inválidos.");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void shouldThrowBusinessExceptionWhenPasswordDoesNotMatchOnLogin() {
        LoginRequest request = new LoginRequest("jane.doe@example.com", "wrongPassword");
        when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("E-mail ou senha inválidos.");

        verify(jwtTokenService, never()).generateToken(any(), any());
    }

    @Test
    void shouldLoginSuccessfullyWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("jane.doe@example.com", "password123");
        when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtTokenService.generateToken(eq(userId), eq("jane.doe@example.com"))).thenReturn("mockedAccessToken");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("mockedAccessToken");
        assertThat(response.refreshToken()).isNotNull();
        assertThat(response.user().name()).isEqualTo("Jane Doe");
        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    @Test
    void shouldCreateNewGoogleUserWhenUserDoesNotExist() {
        GoogleLoginRequest request = new GoogleLoginRequest("googleIdToken");
        GoogleUserInfo googleUserInfo = new GoogleUserInfo("sub123", "google@example.com", "Google User", "https://avatar.url");
        UserEntity createdGoogleUser = UserEntity.builder()
                .name("Google User")
                .email("google@example.com")
                .avatarUrl("https://avatar.url")
                .provider(AuthProvider.GOOGLE)
                .providerId("sub123")
                .build();
        createdGoogleUser.setId(UUID.randomUUID());
        createdGoogleUser.setCreatedAt(Instant.now());

        when(googleTokenVerifierService.verifyToken("googleIdToken")).thenReturn(googleUserInfo);
        when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenReturn(createdGoogleUser);
        when(jwtTokenService.generateToken(eq(createdGoogleUser.getId()), eq("google@example.com"))).thenReturn("googleAccessToken");

        AuthResponse response = authService.loginWithGoogle(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("googleAccessToken");
        assertThat(response.user().provider()).isEqualTo(AuthProvider.GOOGLE);
        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    @Test
    void shouldUpdateExistingGoogleUserWhenUserAlreadyExists() {
        GoogleLoginRequest request = new GoogleLoginRequest("googleIdToken");
        GoogleUserInfo googleUserInfo = new GoogleUserInfo("sub123", "jane.doe@example.com", "Jane Doe", "https://new-avatar.url");

        when(googleTokenVerifierService.verifyToken("googleIdToken")).thenReturn(googleUserInfo);
        when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(userEntity));
        when(userRepository.save(userEntity)).thenReturn(userEntity);
        when(jwtTokenService.generateToken(eq(userId), eq("jane.doe@example.com"))).thenReturn("googleAccessToken");

        AuthResponse response = authService.loginWithGoogle(request);

        assertThat(response).isNotNull();
        assertThat(userEntity.getProviderId()).isEqualTo("sub123");
        assertThat(userEntity.getAvatarUrl()).isEqualTo("https://new-avatar.url");
        verify(userRepository).save(userEntity);
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenRefreshTokenNotFound() {
        RefreshTokenRequest request = new RefreshTokenRequest("nonExistentToken");
        when(refreshTokenRepository.findByTokenWithUser("nonExistentToken")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Sessão inválida ou expirada. Faça login novamente.");
    }

    @Test
    void shouldRevokeAllTokensAndThrowUnauthorizedExceptionWhenTokenIsAlreadyRevoked() {
        RefreshTokenRequest request = new RefreshTokenRequest("revokedToken");
        RefreshTokenEntity storedToken = RefreshTokenEntity.builder()
                .token("revokedToken")
                .user(userEntity)
                .revoked(true)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        when(refreshTokenRepository.findByTokenWithUser("revokedToken")).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Sessão inválida ou expirada. Faça login novamente.");

        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }

    @Test
    void shouldRevokeTokenAndThrowUnauthorizedExceptionWhenTokenIsExpired() {
        RefreshTokenRequest request = new RefreshTokenRequest("expiredToken");
        RefreshTokenEntity storedToken = RefreshTokenEntity.builder()
                .token("expiredToken")
                .user(userEntity)
                .revoked(false)
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
        when(refreshTokenRepository.findByTokenWithUser("expiredToken")).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Sessão expirada. Faça login novamente.");

        assertThat(storedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(storedToken);
    }

    @Test
    void shouldRefreshTokensSuccessfullyWhenTokenIsValid() {
        RefreshTokenRequest request = new RefreshTokenRequest("validToken");
        RefreshTokenEntity storedToken = RefreshTokenEntity.builder()
                .token("validToken")
                .user(userEntity)
                .revoked(false)
                .expiresAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();
        when(refreshTokenRepository.findByTokenWithUser("validToken")).thenReturn(Optional.of(storedToken));
        when(jwtTokenService.generateToken(eq(userId), eq("jane.doe@example.com"))).thenReturn("newAccessToken");

        AuthResponse response = authService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("newAccessToken");
        assertThat(storedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(storedToken);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(any(RefreshTokenEntity.class));
    }

    @Test
    void shouldRevokeTokenWhenLoggingOut() {
        RefreshTokenRequest request = new RefreshTokenRequest("activeToken");
        RefreshTokenEntity storedToken = RefreshTokenEntity.builder()
                .token("activeToken")
                .user(userEntity)
                .revoked(false)
                .expiresAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();
        when(refreshTokenRepository.findByTokenWithUser("activeToken")).thenReturn(Optional.of(storedToken));

        authService.logout(request);

        assertThat(storedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(storedToken);
    }

    @Test
    void shouldDoNothingWhenTokenNotFoundOnLogout() {
        RefreshTokenRequest request = new RefreshTokenRequest("unknownToken");
        when(refreshTokenRepository.findByTokenWithUser("unknownToken")).thenReturn(Optional.empty());

        authService.logout(request);

        verify(refreshTokenRepository, never()).save(any());
    }
}
