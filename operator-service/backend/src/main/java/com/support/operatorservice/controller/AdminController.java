package com.support.operatorservice.controller;

import com.support.operatorservice.dto.CreateUserRequest;
import com.support.operatorservice.dto.UserDto;
import com.support.operatorservice.entity.User;
import com.support.operatorservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final UserService userService;
    
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.findAll().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(UserDto.fromEntity(user));
    }
    
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        User.Role role = User.Role.valueOf(request.getRole());
        User user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getFullName(),
                role
        );
        return ResponseEntity.ok(UserDto.fromEntity(user));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @RequestBody CreateUserRequest request
    ) {
        User.Role role = request.getRole() != null ? User.Role.valueOf(request.getRole()) : null;
        User user = userService.updateUser(id, request.getFullName(), role, true);
        return ResponseEntity.ok(UserDto.fromEntity(user));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<UserDto> toggleUserActive(@PathVariable Long id) {
        User user = userService.findById(id);
        User updatedUser = userService.updateUser(id, null, null, !user.getActive());
        return ResponseEntity.ok(UserDto.fromEntity(updatedUser));
    }
}
