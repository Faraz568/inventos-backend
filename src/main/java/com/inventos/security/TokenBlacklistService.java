package com.inventos.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void    revoke(String jti, Date expiry) { blacklist.put(jti, expiry.getTime()); }
    public boolean isRevoked(String jti)           { return blacklist.containsKey(jti); }

    @Scheduled(fixedDelay = 600_000)
    public void purge() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(e -> e.getValue() < now);
    }
}
