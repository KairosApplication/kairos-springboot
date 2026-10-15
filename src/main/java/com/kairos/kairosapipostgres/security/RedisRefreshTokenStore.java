package com.kairos.kairosapipostgres.security;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Repository
public class RedisRefreshTokenStore {
    private static final DefaultRedisScript<Long> CREATE = script("create-session", Long.class);
    private static final DefaultRedisScript<List> REFRESH = script("refresh-session", List.class);
    private static final DefaultRedisScript<Long> REVOKE = script("revoke-session", Long.class);
    private final StringRedisTemplate redis;

    public RedisRefreshTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private static <T> DefaultRedisScript<T> script(String name, Class<T> type) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/" + name + ".lua"));
        script.setResultType(type);
        return script;
    }

    public Instant create(long userId, RefreshToken token, Duration ttl) {
        Long expires = redis.execute(CREATE, List.of(key(token.sessionId())),
                Long.toString(userId), token.hash(), Long.toString(ttl.toMillis()));
        if (expires == null || expires == 0) {
            throw new IllegalStateException("Could not create authentication session");
        }
        return Instant.ofEpochMilli(expires);
    }

    public Session inspect(RefreshToken token) {
        return exchange(token, "");
    }

    public Session rotate(RefreshToken previous, RefreshToken next) {
        if (!previous.sessionId().equals(next.sessionId())) {
            throw new IllegalArgumentException("Cannot rotate across sessions");
        }
        return exchange(previous, next.hash());
    }

    private Session exchange(RefreshToken token, String nextHash) {
        List<?> result = redis.execute(REFRESH, List.of(key(token.sessionId())), token.hash(), nextHash);
        if (result == null || result.size() != 2) {
            throw new BadCredentialsException("Invalid or expired refresh session");
        }
        return new Session(Long.parseLong(result.get(0).toString()),
                Instant.ofEpochMilli(Long.parseLong(result.get(1).toString())));
    }

    public void revoke(RefreshToken token) {
        redis.execute(REVOKE, List.of(key(token.sessionId())), token.hash());
    }

    public void revokeAuthenticatedSession(String sessionId) {
        if (sessionId == null || !sessionId.matches("[a-f0-9]{32}")) {
            throw new BadCredentialsException("Invalid session");
        }
        redis.execute(REVOKE, List.of(key(sessionId)), "");
    }

    public static String key(String sessionId) {
        return "kairos:auth:{" + sessionId + "}";
    }

    public record Session(long userId, Instant expiresAt) {
    }
}
