package com.encore.encoreapi.user;

import com.encore.encoreapi.config.Role;
import jakarta.validation.constraints.NotNull;

public class UpdateRoleRequest {

    @NotNull
    private Role role;

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}