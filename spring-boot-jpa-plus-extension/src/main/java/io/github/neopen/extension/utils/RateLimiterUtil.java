/*
 * Copyright 2026 neopen (余一叶知秋尽)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.neopen.extension.utils;

import com.google.common.util.concurrent.RateLimiter;
import io.github.neopen.extension.cache.LRUCache;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Guava {@link RateLimiter} 的本地限流工具。
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class RateLimiterUtil {

    /**
     * 工具类禁止实例化。
     */
    private RateLimiterUtil() {
    }

    /** 每秒基准许可数。 */
    public static int sec_sum = 5;

    /** 每分钟基准许可数，预留给限流策略扩展。 */
    public static int min_sum = 20;

    /** 按业务键缓存限流器。 */
    private final static Map<String, RateLimiter> rateLimiterMap = new LRUCache<>(800, 200);

    /**
     * 获取指定业务键对应的限流器。
     *
     * <p>首次访问时创建限流器，许可速率为每秒基准许可数的两倍，
     * 预热时间为两秒。</p>
     *
     * @param key 限流业务键
     * @return 已缓存或新创建的限流器
     */
    public static RateLimiter getRateLimiter(String key) {
        return rateLimiterMap.computeIfAbsent(key, k -> RateLimiter.create(sec_sum * 2, 2, TimeUnit.SECONDS));
    }

    /**
     * 尝试为用户和方法的组合获取一个许可。
     *
     * @param userNo 用户标识
     * @param method 方法或资源标识
     * @return 立即获得许可时返回 {@code true}
     */
    public static boolean tryAcquire(String userNo, String method) {
        return getRateLimiter(userNo + "_" + method).tryAcquire();
    }


    /**
     * 演示限流器的基本用法。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {

        RateLimiter commRateLimiter = RateLimiter.create(sec_sum, 1, TimeUnit.SECONDS);
        if (!commRateLimiter.tryAcquire(RateLimiterUtil.sec_sum, TimeUnit.SECONDS))
            System.out.println("操作频繁，稍后重试");
    }

}
