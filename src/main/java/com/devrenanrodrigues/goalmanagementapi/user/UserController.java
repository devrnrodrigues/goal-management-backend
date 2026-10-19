package com.devrenanrodrigues.goalmanagementapi.user;

import com.devrenanrodrigues.goalmanagementapi.common.security.AuthenticatedUser;
import com.devrenanrodrigues.goalmanagementapi.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        UserResponse response = userService.getCurrentUser(authenticatedUser.id());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> listUsers(Pageable pageable) {
        Page<UserResponse> response = userService.listUsers(pageable);
        return ResponseEntity.ok(response);
    }
}
