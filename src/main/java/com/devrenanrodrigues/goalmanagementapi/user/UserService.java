package com.devrenanrodrigues.goalmanagementapi.user;

import com.devrenanrodrigues.goalmanagementapi.common.exception.BusinessException;
import com.devrenanrodrigues.goalmanagementapi.common.exception.ResourceNotFoundException;
import com.devrenanrodrigues.goalmanagementapi.user.dto.UpdateProfileRequest;
import com.devrenanrodrigues.goalmanagementapi.user.dto.UserResponse;
import com.devrenanrodrigues.goalmanagementapi.user.enums.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        return UserResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(String search, Pageable pageable) {
        return userRepository.searchUsers(search, pageable).map(UserResponse::fromEntity);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        user.setName(request.name().trim());

        if (request.avatarUrl() != null) {
            String trimmedAvatar = request.avatarUrl().trim();
            user.setAvatarUrl(trimmedAvatar.isEmpty() ? null : trimmedAvatar);
        }

        if (request.theme() != null && !request.theme().isBlank()) {
            user.setTheme(request.theme().trim().toUpperCase());
        }

        if (request.emailNotifications() != null) {
            user.setEmailNotifications(request.emailNotifications());
        }

        if (request.weeklySummary() != null) {
            user.setWeeklySummary(request.weeklySummary());
        }

        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (user.getProvider() != AuthProvider.LOCAL) {
                throw new BusinessException("Contas vinculadas ao Google não podem alterar senha.");
            }
            if (request.currentPassword() == null || request.currentPassword().isBlank()) {
                throw new BusinessException("A senha atual é obrigatória para definir uma nova senha.");
            }
            if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new BusinessException("A senha atual informada está incorreta.");
            }
            if (request.newPassword().trim().length() < 6) {
                throw new BusinessException("A nova senha deve ter no mínimo 6 caracteres.");
            }
            user.setPasswordHash(passwordEncoder.encode(request.newPassword().trim()));
        }

        UserEntity saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }
}
