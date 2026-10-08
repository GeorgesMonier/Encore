package com.encore.encoreapi.user;

import com.encore.encoreapi.config.Role;

import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String name,
        Role role,
        boolean enabled,
        boolean totpEnabled
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.isEnabled(),
                user.isTotpEnabled()
        );
    }
}