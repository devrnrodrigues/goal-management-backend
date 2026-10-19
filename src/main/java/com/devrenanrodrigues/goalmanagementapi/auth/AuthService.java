package com.devrenanrodrigues.goalmanagementapi.auth;

import com.devrenanrodrigues.goalmanagementapi.auth.dto.AuthResponse;
import com.devrenanrodrigues.goalmanagementapi.auth.dto.LoginRequest;
import com.devrenanrodrigues.goalmanagementapi.auth.dto.RegisterRequest;
import com.devrenanrodrigues.goalmanagementapi.common.exception.BusinessException;
import com.devrenanrodrigues.goalmanagementapi.common.security.JwtTokenService;
import com.devrenanrodrigues.goalmanagementapi.user.UserEntity;
import com.devrenanrodrigues.goalmanagementapi.user.UserRepository;
import com.devrenanrodrigues.goalmanagementapi.user.dto.UserResponse;
import com.devrenanrodrigues.goalmanagementapi.user.enums.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

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
        String token = jwtTokenService.generateToken(savedUser.getId(), savedUser.getEmail());

        return AuthResponse.of(token, UserResponse.fromEntity(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BusinessException("E-mail ou senha inválidos."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("E-mail ou senha inválidos.");
        }

        String token = jwtTokenService.generateToken(user.getId(), user.getEmail());
        return AuthResponse.of(token, UserResponse.fromEntity(user));
    }
}
