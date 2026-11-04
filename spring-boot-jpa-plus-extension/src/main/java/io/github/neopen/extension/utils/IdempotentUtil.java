package io.github.neopen.extension.utils;

import io.github.neopen.extension.cache.LRUCache;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

/**
 * <b>  幂等性校验    </b>
 * <p>
 *
 * @Author neopen
 * @GitHub https://github.com/neopen/spring-boot-jpa-plus
 * @Date 2021/6/30 10:20
 */
@Slf4j
public class IdempotentUtil {

    // 缓存参数校验token
    public final static Map<String, Object> tokenMap = new LRUCache<>(200, 20);

    // 校验并移除key
    public static boolean checkToken(HttpServletRequest request, String token) {
        log.info("====checkToken=====" + request.getRequestedSessionId());

        return StringUtils.hasText(token) && tokenMap.remove(request.getRequestedSessionId(), token);
    }

    // 获得并添加key
    public static String getToken(HttpServletRequest request) {
        String token = UUID.randomUUID().toString();
        log.info("{}====checkToken====={}", request.getRequestedSessionId(), token);

        tokenMap.put(request.getRequestedSessionId(), token);

        return token;
    }

}
