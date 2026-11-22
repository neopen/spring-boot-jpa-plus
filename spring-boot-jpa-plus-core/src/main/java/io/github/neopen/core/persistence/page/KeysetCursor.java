package io.github.neopen.core.persistence.page;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * 游标（keyset）分页位置。
 *
 * <p>记录上一页最后一行在排序键上的取值，下一页查询以此为起点，
 * 通过 {@code WHERE (sortKey, id) > (?, ?)} 定位，避免 offset 分页
 * 在深翻页时的性能退化与数据漂移。</p>
 *
 * <p>游标可以通过 {@link #encode()} 输出为 URL 安全的字符串返回给
 * 前端，再由 {@link #decode(String)} 还原。还原后的值均为字符串，
 * 查询执行器会按排序字段的 Java 类型再做转换。</p>
 *
 * <pre>{@code
 * KeysetCursor cursor = KeysetCursor.of(100L, "2026-01-01T10:00:00");
 *
 * String token = cursor.encode();
 * KeysetCursor restored = KeysetCursor.decode(token);
 * }</pre>
 *
 * @author neopen
 * @see KeysetPageParams
 * @see KeysetPage
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class KeysetCursor {

    /** 值之间的分隔符，使用不会出现在常规业务值中的空字符。 */
    private static final String SEPARATOR = "\u0000";

    /** 排序键取值，顺序与排序项一致。 */
    private final List<Object> values;

    /**
     * 创建游标。
     *
     * @param values 排序键取值
     */
    private KeysetCursor(List<?> values) {
        this.values = List.copyOf(values);
    }

    /**
     * 创建游标。
     *
     * @param values 排序键取值，顺序与排序项一致
     * @return 游标
     * @throws IllegalArgumentException 取值为空或包含 {@code null} 时抛出
     */
    public static KeysetCursor of(Object... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException(
                    "Cursor values must not be empty"
            );
        }

        List<Object> list = new ArrayList<>(values.length);

        for (Object value : values) {
            if (value == null) {
                throw new IllegalArgumentException(
                        "Cursor value must not be null"
                );
            }

            list.add(value);
        }

        return new KeysetCursor(list);
    }

    /**
     * 获取排序键取值。
     *
     * @return 不可变的取值列表
     */
    public List<Object> getValues() {
        return values;
    }

    /**
     * 编码为 URL 安全的字符串。
     *
     * @return 游标字符串
     */
    public String encode() {
        String joined = String.join(
                SEPARATOR,
                values.stream().map(String::valueOf).toList()
        );

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(joined.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 从字符串还原游标。
     *
     * @param encoded 游标字符串
     * @return 游标，取值为解码后的字符串
     * @throws IllegalArgumentException 字符串为空或不是合法游标时抛出
     */
    public static KeysetCursor decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalArgumentException(
                    "Cursor must not be blank"
            );
        }

        String joined;

        try {
            joined = new String(
                    Base64.getUrlDecoder().decode(encoded.trim()),
                    StandardCharsets.UTF_8
            );
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid cursor", ex);
        }

        if (joined.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cursor values must not be empty"
            );
        }

        String[] parts = joined.split(SEPARATOR, -1);

        return new KeysetCursor(Arrays.asList(parts));
    }
}
