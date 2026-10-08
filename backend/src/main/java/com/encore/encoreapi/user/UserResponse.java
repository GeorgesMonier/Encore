package com.encore.encoreapi.user;

import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String email;
    private String name;

    public UserResponse(UUID id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
}