package com.encore.encoreapi.user;

import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String email;
    private String name;
    private boolean totpEnabled;

    public UserResponse(UUID id, String email, String name, boolean totpEnabled) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.totpEnabled = totpEnabled;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public boolean isTotpEnabled() { return totpEnabled; }
}