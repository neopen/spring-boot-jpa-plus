package io.github.neopen.extension.utils;

import io.github.neopen.extension.cache.LRUCache;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

/**
 * 基于会话的一次性令牌工具。
 *
 * <p>令牌在首次成功校验时移除，用于防止重复提交。</p>
 */
@Slf4j
public class IdempotentUtil {

    /** 以会话编号为键缓存一次性令牌。 */
    public final static Map<String, Object> tokenMap = new LRUCache<>(200, 20);

    /**
     * 校验并移除当前会话的一次性令牌。
     *
     * @param request 当前 HTTP 请求
     * @param token   待校验令牌
     * @return 令牌非空且与当前会话匹配时返回 {@code true}
     */
    public static boolean checkToken(HttpServletRequest request, String token) {
        log.info("====checkToken=====" + request.getRequestedSessionId());

        return StringUtils.hasText(token) && tokenMap.remove(request.getRequestedSessionId(), token);
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
