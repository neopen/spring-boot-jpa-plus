package io.github.neopen.jpa.utils;

import cn.hutool.core.util.StrUtil;
import io.github.neopen.jpa.cache.LRUCache;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.UUID;

/**
 * Process-local, session-scoped one-time token helper.
 *
 * <p>This is not a distributed idempotency mechanism. Multi-instance deployments
 * should use a shared store with atomic consume semantics.</p>
 */
public final class IdempotentUtil {

    private static final Map<String, String> TOKEN_MAP = new LRUCache<>(200, 20);

    private IdempotentUtil() {
    }

    public static synchronized boolean checkToken(HttpServletRequest request, String token) {
        if (request == null || StrUtil.isBlank(token)) {
            return false;
        }
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        return token.equals(TOKEN_MAP.remove(session.getId()));
    }

    public static synchronized String getToken(HttpServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String sessionId = request.getSession(true).getId();
        String token = UUID.randomUUID().toString();
        TOKEN_MAP.put(sessionId, token);
        return token;
    }
}
