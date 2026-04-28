package com.school.demo.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Simple in-memory LRU + TTL cache for generated SQL per question.
 * Keeps dependencies minimal (no external cache libs).
 */
@Component
public class SqlCache {

    private record Entry(String sql, long createdAtEpochSeconds) {}

    private final boolean enabled;
    private final int maxSize;
    private final long ttlSeconds;

    // accessOrder=true => LRU
    private final Map<String, Entry> lru;

    public SqlCache(
            @Value("${llm.sql-cache.enabled:true}") boolean enabled,
            @Value("${llm.sql-cache.max-size:200}") int maxSize,
            @Value("${llm.sql-cache.ttl-seconds:3600}") long ttlSeconds) {
        this.enabled = enabled;
        this.maxSize = Math.max(10, maxSize);
        this.ttlSeconds = Math.max(10, ttlSeconds);
        this.lru = new LinkedHashMap<>(128, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
                return size() > SqlCache.this.maxSize;
            }
        };
    }

    public Optional<String> get(String questionKey) {
        if (!enabled) {
            return Optional.empty();
        }
        long now = Instant.now().getEpochSecond();
        synchronized (lru) {
            Entry e = lru.get(questionKey);
            if (e == null) {
                return Optional.empty();
            }
            if ((now - e.createdAtEpochSeconds) > ttlSeconds) {
                lru.remove(questionKey);
                return Optional.empty();
            }
            return Optional.of(e.sql);
        }
    }

    public void put(String questionKey, String sql) {
        if (!enabled) {
            return;
        }
        if (questionKey == null || questionKey.isBlank() || sql == null || sql.isBlank()) {
            return;
        }
        long now = Instant.now().getEpochSecond();
        synchronized (lru) {
            lru.put(questionKey, new Entry(sql, now));
        }
    }

    public int size() {
        synchronized (lru) {
            return lru.size();
        }
    }
}

