package com.support.operatorservice.controller;

import com.support.operatorservice.dto.*;
import com.support.operatorservice.entity.User;
import com.support.operatorservice.service.AuthService;
import com.support.operatorservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final UserService userService;
    
    @GetMapping("/system-initialized")
    public ResponseEntity<Boolean> isSystemInitialized() {
        return ResponseEntity.ok(userService.hasAnyUsers());
    }
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerInitialAdmin(@Valid @RequestBody RegisterRequest request) {
        User admin = userService.registerInitialAdmin(
                request.getUsername(),
                request.getPassword(),
                request.getFullName()
        );
        
        String token = authService.authenticate(request.getUsername(), request.getPassword());
        
        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .user(UserDto.fromEntity(admin))
                .build());
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.authenticate(request.getUsername(), request.getPassword());
        User user = authService.getCurrentUser(request.getUsername());
        
        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .user(UserDto.fromEntity(user))
                .build());
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserDto.fromEntity(user));
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<UserDto> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        User updatedUser = userService.changePassword(user.getId(), request.getNewPassword());
        return ResponseEntity.ok(UserDto.fromEntity(updatedUser));
    }
}
