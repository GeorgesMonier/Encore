package com.encore.encoreapi.user;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String email;
    private String name;
    private boolean totpEnabled;
    private LocalDateTime deletionScheduledAt;

    public UserResponse(UUID id, String email, String name, boolean totpEnabled,
                        LocalDateTime deletionRequestedAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.totpEnabled = totpEnabled;
        this.deletionScheduledAt = deletionRequestedAt == null ? null : deletionRequestedAt.plusDays(14);
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public boolean isTotpEnabled() { return totpEnabled; }
    public LocalDateTime getDeletionScheduledAt() { return deletionScheduledAt; }
}