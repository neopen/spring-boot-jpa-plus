package io.github.neopen.jpa.utils;

import com.google.common.util.concurrent.RateLimiter;
import io.github.neopen.jpa.cache.LRUCache;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <b>   限流   </b>
 * <p>
 *
 * @Author He.hp
 * @Email hehp@citycloud.com.cn
 * @Date 2021/6/30 14:38
 */
public class RateLimiterUtil {

    // 一秒几次请求
    public static int sec_sum = 5;
    // 一分钟几次请求
    public static int min_sum = 20;

    private final static Map<String, RateLimiter> rateLimiterMap = new LRUCache<>(800, 200);

    /**
     * <b> 获得限速器
     * <p>
     *
     * @param key
     * @return com.google.common.util.concurrent.RateLimiter
     * @Author He.hp
     * @Email hehp@citycloud.com.cn
     * @Date 2021/7/8 16:04
     */
    public static RateLimiter getRateLimiter(String key) {
        /**
         * 创建一个限速器，每1秒，产生5个令牌
         */
        return rateLimiterMap.computeIfAbsent(key, k -> RateLimiter.create(sec_sum * 2, 2, TimeUnit.SECONDS));
    }

    // 获取令牌
    public static boolean tryAcquire(String userNo, String method) {
        return getRateLimiter(userNo + "_" + method).tryAcquire();
    }


    public static void main(String[] args) {

        RateLimiter commRateLimiter = RateLimiter.create(sec_sum, 1, TimeUnit.SECONDS);
        if (!commRateLimiter.tryAcquire(RateLimiterUtil.sec_sum, TimeUnit.SECONDS))
            System.out.println("操作频繁，稍后重试");
    }

}
