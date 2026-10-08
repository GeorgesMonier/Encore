package com.encore.encoreapi.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ApiRateLimitService {

    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_TRACKED_KEYS = 20_000;

    private final Map<String, Window> windows = new LinkedHashMap<>();

    public synchronized Decision tryAcquire(String key, int limit) {
        long now = System.currentTimeMillis();
        Window window = windows.get(key);

        if (window == null || now - window.startedAt >= WINDOW_MILLIS) {
            makeRoomIfNeeded(now);
            window = new Window(now);
            windows.put(key, window);
        }

        if (window.count >= limit) {
            long retryAfterSeconds = Math.max(1, (WINDOW_MILLIS - (now - window.startedAt) + 999) / 1000);
            return new Decision(false, 0, retryAfterSeconds);
        }

        window.count++;
        return new Decision(true, limit - window.count, 0);
    }

    @Scheduled(fixedDelay = WINDOW_MILLIS)
    public synchronized void removeExpiredWindows() {
        makeRoomIfNeeded(System.currentTimeMillis());
    }

    private void makeRoomIfNeeded(long now) {
        Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().startedAt >= WINDOW_MILLIS) {
                iterator.remove();
            }
        }

        while (windows.size() >= MAX_TRACKED_KEYS) {
            Iterator<String> oldest = windows.keySet().iterator();
            if (!oldest.hasNext()) return;
            oldest.next();
            oldest.remove();
        }
    }

    public record Decision(boolean allowed, int remaining, long retryAfterSeconds) {}

    private static final class Window {
        private final long startedAt;
        private int count;

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
