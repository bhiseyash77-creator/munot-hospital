package com.munot.hospital.security;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** Simple in-memory brute-force protection: 5 failures lock the account key for 15 minutes. */
@Service
public class LoginAttemptService {
    private static final int MAX = 5;
    private static final long LOCK_SECONDS = 15 * 60L;

    private record Attempt(int count, Instant last) {}

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String key) {
        Attempt a = attempts.get(key.toLowerCase());
        if (a == null) return false;
        if (a.last().plusSeconds(LOCK_SECONDS).isBefore(Instant.now())) { attempts.remove(key.toLowerCase()); return false; }
        return a.count() >= MAX;
    }

    public void failed(String key) {
        attempts.merge(key.toLowerCase(), new Attempt(1, Instant.now()), (o, n) -> new Attempt(o.count() + 1, n.last()));
    }

    public void succeeded(String key) { attempts.remove(key.toLowerCase()); }
}
