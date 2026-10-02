package com.encore.encoreapi.security;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BLOCK_MINUTES = 15;

    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public void loginFailed(String email) {
        AttemptInfo info = attempts.getOrDefault(email, new AttemptInfo(0, null));
        info.count++;
        if (info.count >= MAX_ATTEMPTS) {
            info.blockedUntil = LocalDateTime.now().plusMinutes(BLOCK_MINUTES);
        }
        attempts.put(email, info);
    }

    public void loginSucceeded(String email) {
        attempts.remove(email);
    }

    public boolean isBlocked(String email) {
        AttemptInfo info = attempts.get(email);
        if (info == null || info.blockedUntil == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(info.blockedUntil)) {
            attempts.remove(email);
            return false;
        }
        return true;
    }

    private static class AttemptInfo {
        int count;
        LocalDateTime blockedUntil;

        AttemptInfo(int count, LocalDateTime blockedUntil) {
            this.count = count;
            this.blockedUntil = blockedUntil;
        }
    }
}