package com.hengline.jpa.utils;

import cn.hutool.core.util.StrUtil;
import com.hengline.jpa.cache.LRUCache;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

/**
 * <b>  幂等性校验    </b>
 * <p>
 *
 * @Author He.hp
 * @Email hehp@citycloud.com.cn
 * @Date 2021/6/30 10:20
 */
@Slf4j
public class IdempotentUtil {

    // 缓存参数校验token
    public final static Map<String, Object> tokenMap = new LRUCache<>(200, 20);

    // 校验并移除key
    public static boolean checkToken(HttpServletRequest request, String token) {
        log.info("====checkToken=====" + request.getRequestedSessionId());

        return StrUtil.isNotBlank(token) && tokenMap.remove(request.getRequestedSessionId(), token);
    }

    // 获得并添加key
    public static String getToken(HttpServletRequest request) {
        String token = UUID.randomUUID().toString();
        log.info("{}====checkToken====={}", request.getRequestedSessionId(), token);

        tokenMap.put(request.getRequestedSessionId(), token);

        return token;
    }

}
