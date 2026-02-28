package com.support.operatorservice.dto;

import com.support.operatorservice.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String fullName;
    private String role;
    private Boolean firstLogin;
    private Boolean active;
    private Boolean superAdmin;
    private LocalDateTime createdAt;
    
    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .firstLogin(user.getFirstLogin())
                .active(user.getActive())
                .superAdmin(user.getSuperAdmin())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
