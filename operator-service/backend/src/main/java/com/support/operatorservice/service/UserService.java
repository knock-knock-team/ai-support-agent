package com.support.operatorservice.service;

import com.support.operatorservice.entity.User;
import com.support.operatorservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    public boolean hasAnyUsers() {
        return userRepository.count() > 0;
    }
    
    public Optional<User> getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            var principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return Optional.of((User) principal);
            }
            if (principal instanceof String) {
                return userRepository.findByUsername((String) principal);
            }
        }
        return Optional.empty();
    }
    
    @Transactional
    public User registerInitialAdmin(String username, String password, String fullName) {
        if (hasAnyUsers()) {
            throw new RuntimeException("Initial registration is only allowed when no users exist");
        }
        
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        
        User superAdmin = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .fullName(fullName)
                .role(User.Role.ROLE_ADMIN)
                .firstLogin(true)
                .active(true)
                .superAdmin(true)
                .build();
        
        return userRepository.save(superAdmin);
    }
    
    @Transactional
    public User createUser(String username, String password, String fullName, User.Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .fullName(fullName)
                .role(role)
                .firstLogin(true)
                .active(true)
                .superAdmin(false)
                .build();
        
        return userRepository.save(user);
    }
    
    @Transactional
    public User changePassword(Long userId, String newPassword) {
        User user = findById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFirstLogin(false);
        return userRepository.save(user);
    }
    
    @Transactional
    public User updateUser(Long id, String fullName, User.Role role, Boolean active) {
        User user = findById(id);
        if (fullName != null) user.setFullName(fullName);
        if (role != null) user.setRole(role);
        if (active != null) user.setActive(active);
        return userRepository.save(user);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        User userToDelete = findById(id);
        
        // Super Admin cannot be deleted
        if (userToDelete.getSuperAdmin()) {
            throw new RuntimeException("Cannot delete the super admin");
        }
        
        Optional<User> currentUserOpt = getCurrentUser();
        if (currentUserOpt.isEmpty()) {
            throw new RuntimeException("Unauthorized");
        }
        
        User currentUser = currentUserOpt.get();
        
        // Super Admin can delete anyone (except super admin itself, which is checked above)
        if (currentUser.getSuperAdmin()) {
            userRepository.deleteById(id);
            return;
        }
        
        // Regular Admin can only delete Operators, not other Admins
        if (currentUser.getRole() == User.Role.ROLE_ADMIN) {
            if (userToDelete.getRole() == User.Role.ROLE_ADMIN) {
                throw new RuntimeException("Admins can only delete operators, not other admins");
            }
            userRepository.deleteById(id);
            return;
        }
        
        // Operators cannot delete anyone
        throw new RuntimeException("Insufficient permissions to delete users");
    }
}
