package io.github.neopen.extension.utils;

import io.github.neopen.extension.cache.LRUCache;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 幂等性与一次性令牌工具。
 *
 * <p>提供两种能力：按会话缓存一次性令牌（首次成功校验时移除），
 * 以及按业务键在时间窗口内去重（{@link #acquire(String, long)}）。
 * 均基于本地内存实现，多实例部署时需要替换为分布式存储。</p>
 */
@Slf4j
public class IdempotentUtil {

    /** 以会话编号为键缓存一次性令牌。 */
    public final static Map<String, Object> tokenMap = new LRUCache<>(200, 20);

    /** 业务键到过期时间戳的缓存，时间戳使用毫秒。 */
    private final static Map<String, Long> keyExpiry =
            Collections.synchronizedMap(new LRUCache<>(2000, 200));

    /**
     * 尝试占用业务键。
     *
     * <p>同一业务键在时间窗口内只允许占用一次，用于按订单号、
     * 请求号等业务标识做幂等去重。</p>
     *
     * @param key        业务键，不能为空
     * @param ttlSeconds 时间窗口秒数；小于等于 0 表示长期有效
     * @return 首次占用时返回 {@code true}
     * @throws IllegalArgumentException 业务键为空
     */
    public static boolean acquire(String key, long ttlSeconds) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException(
                    "Idempotent key must not be blank"
            );
        }

        long ttlMillis = TimeUnit.SECONDS.toMillis(
                Math.max(ttlSeconds, 0L)
        );

        AtomicBoolean acquired = new AtomicBoolean(false);

        keyExpiry.compute(key, (ignored, expiry) -> {
            long now = System.currentTimeMillis();

            if (expiry != null && expiry > now) {
                return expiry;
            }

            acquired.set(true);

            return ttlMillis <= 0
                    ? Long.MAX_VALUE
                    : now + ttlMillis;
        });

        return acquired.get();
    }

    /**
     * 校验并移除指定键下的一次性令牌。
     *
     * @param key   令牌缓存键，例如会话编号
     * @param token 待校验令牌
     * @return 令牌匹配并成功移除时返回 {@code true}
     */
    public static boolean consume(String key, String token) {
        return StringUtils.hasText(key)
                && StringUtils.hasText(token)
                && tokenMap.remove(key, token);
    }

    /**
     * 校验并移除当前会话的一次性令牌。
     *
     * @param request 当前 HTTP 请求
     * @param token   待校验令牌
     * @return 令牌非空且与当前会话匹配时返回 {@code true}
     */
    public static boolean checkToken(HttpServletRequest request, String token) {
        log.info("====checkToken=====" + request.getRequestedSessionId());

        return consume(request.getRequestedSessionId(), token);
    }

    /**
     * 为当前会话生成并缓存一次性令牌。
     *
     * @param request 当前 HTTP 请求
     * @return 新生成的随机令牌
     */
    public static String getToken(HttpServletRequest request) {
        String token = UUID.randomUUID().toString();
        log.info("{}====checkToken====={}", request.getRequestedSessionId(), token);

        tokenMap.put(request.getRequestedSessionId(), token);

        return token;
    }

}
