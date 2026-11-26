package com.devrenanrodrigues.goalmanagementapi.user;

import com.devrenanrodrigues.goalmanagementapi.common.exception.BusinessException;
import com.devrenanrodrigues.goalmanagementapi.common.exception.ResourceNotFoundException;
import com.devrenanrodrigues.goalmanagementapi.user.dto.UpdateProfileRequest;
import com.devrenanrodrigues.goalmanagementapi.user.dto.UserResponse;
import com.devrenanrodrigues.goalmanagementapi.user.enums.AuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userEntity = UserEntity.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .passwordHash("encodedCurrentPassword")
                .provider(AuthProvider.LOCAL)
                .theme("LIGHT")
                .emailNotifications(true)
                .weeklySummary(true)
                .build();
        userEntity.setId(userId);
        userEntity.setCreatedAt(Instant.now());
    }

    @Test
    void shouldReturnUserResponseWhenUserExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        UserResponse response = userService.getCurrentUser(userId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.name()).isEqualTo("John Doe");
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        verify(userRepository).findById(userId);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado.");

        verify(userRepository).findById(userId);
    }

    @Test
    void shouldReturnPagedUserResponsesWhenListingUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserEntity> pagedUsers = new PageImpl<>(List.of(userEntity), pageable, 1);
        when(userRepository.searchUsers(eq("John"), eq(pageable))).thenReturn(pagedUsers);

        Page<UserResponse> result = userService.listUsers("John", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("John Doe");
        verify(userRepository).searchUsers("John", pageable);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUpdatingNonExistingUser() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Jane Doe",
                null,
                "DARK",
                false,
                false,
                null,
                null
        );
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado.");

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldUpdateBasicProfileFieldsSuccessfully() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Jane Doe",
                "https://avatar.url/jane.png",
                "dark",
                false,
                false,
                null,
                null
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateProfile(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Jane Doe");
        assertThat(response.avatarUrl()).isEqualTo("https://avatar.url/jane.png");
        assertThat(response.theme()).isEqualTo("DARK");
        assertThat(response.emailNotifications()).isFalse();
        assertThat(response.weeklySummary()).isFalse();
        verify(userRepository).save(userEntity);
    }

    @Test
    void shouldClearAvatarUrlWhenEmptyStringProvided() {
        userEntity.setAvatarUrl("https://avatar.url/old.png");
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Jane Doe",
                "   ",
                null,
                null,
                null,
                null,
                null
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateProfile(userId, request);

        assertThat(response.avatarUrl()).isNull();
        verify(userRepository).save(userEntity);
    }

    @Test
    void shouldThrowBusinessExceptionWhenGoogleUserTriesToChangePassword() {
        userEntity.setProvider(AuthProvider.GOOGLE);
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Google User",
                null,
                null,
                null,
                null,
                "oldPass",
                "newPassword123"
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Contas vinculadas ao Google não podem alterar senha.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessExceptionWhenCurrentPasswordIsBlankWhileChangingPassword() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "John Doe",
                null,
                null,
                null,
                null,
                "   ",
                "newPassword123"
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A senha atual é obrigatória para definir uma nova senha.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessExceptionWhenCurrentPasswordDoesNotMatch() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "John Doe",
                null,
                null,
                null,
                null,
                "wrongPassword",
                "newPassword123"
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches("wrongPassword", "encodedCurrentPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A senha atual informada está incorreta.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessExceptionWhenNewPasswordIsLessThanSixCharacters() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "John Doe",
                null,
                null,
                null,
                null,
                "currentPassword",
                "12345"
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches("currentPassword", "encodedCurrentPassword")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A nova senha deve ter no mínimo 6 caracteres.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldUpdatePasswordSuccessfullyWhenAllConditionsMet() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "John Doe",
                null,
                null,
                null,
                null,
                "currentPassword",
                "newValidPassword"
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches("currentPassword", "encodedCurrentPassword")).thenReturn(true);
        when(passwordEncoder.encode("newValidPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateProfile(userId, request);

        assertThat(response).isNotNull();
        assertThat(userEntity.getPasswordHash()).isEqualTo("newEncodedPassword");
        verify(passwordEncoder).encode("newValidPassword");
        verify(userRepository).save(userEntity);
    }
}
