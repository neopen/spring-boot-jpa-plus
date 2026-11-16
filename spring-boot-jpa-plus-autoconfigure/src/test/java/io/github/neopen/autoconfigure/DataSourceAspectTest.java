package io.github.neopen.autoconfigure;

import io.github.neopen.core.persistence.datasource.DataSource;
import io.github.neopen.core.persistence.datasource.DataSourceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link DataSourceAspect} 单元测试。
 */
class DataSourceAspectTest {

    /**
     * 清理线程上下文。
     */
    @AfterEach
    void reset() {
        DataSourceContext.clear();
    }

    /**
     * 验证方法上的数据源名称生效。
     *
     * @throws Throwable 方法执行异常
     */
    @Test
    void shouldUseMethodTarget() throws Throwable {
        DataSourceAspect aspect = new DataSourceAspect(properties());
        AnnotatedService service = new AnnotatedService();
        AtomicReference<String> observed = new AtomicReference<>();

        Object result = aspect.around(joinPoint(
                service,
                "read",
                observed
        ));

        assertThat(result).isEqualTo("ok");
        assertThat(observed).hasValue("read");
        assertThat(DataSourceContext.current()).isNull();
    }

    /**
     * 验证类上的只读配置生效。
     *
     * @throws Throwable 方法执行异常
     */
    @Test
    void shouldUseClassReadOnlyTarget() throws Throwable {
        DataSourceAspect aspect = new DataSourceAspect(properties());
        ReadOnlyService service = new ReadOnlyService();
        AtomicReference<String> observed = new AtomicReference<>();

        aspect.around(joinPoint(service, "list", observed));

        assertThat(observed).hasValue("read");
    }

    /**
     * 验证只读关闭时使用写数据源。
     *
     * @throws Throwable 方法执行异常
     */
    @Test
    void shouldUseWriteTargetWhenNotReadOnly() throws Throwable {
        DataSourceAspect aspect = new DataSourceAspect(properties());
        WriteService service = new WriteService();
        AtomicReference<String> observed = new AtomicReference<>();

        aspect.around(joinPoint(service, "save", observed));

        assertThat(observed).hasValue("write");
    }

    /**
     * 创建动态数据源配置。
     *
     * @return 配置
     */
    private JpaPlusProperties.Datasource properties() {
        JpaPlusProperties.Datasource properties =
                new JpaPlusProperties.Datasource();
        properties.setReadTarget("read");
        properties.setWriteTarget("write");

        return properties;
    }

    /**
     * 创建连接点。
     *
     * @param target   目标对象
     * @param method   方法名
     * @param observed 观察到的当前数据源
     * @return 连接点
     * @throws Throwable 构造连接点失败时抛出
     */
    private ProceedingJoinPoint joinPoint(
            Object target,
            String method,
            AtomicReference<String> observed
    ) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                target.getClass().getMethod(method)
        );
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            observed.set(DataSourceContext.current());

            return "ok";
        });

        return joinPoint;
    }

    /** 方法级数据源配置。 */
    static class AnnotatedService {

        /**
         * 查询。
         *
         * @return 结果
         */
        @DataSource("read")
        public String read() {
            return "ok";
        }
    }

    /** 类级只读配置。 */
    @DataSource(readOnly = true)
    static class ReadOnlyService {

        /**
         * 查询。
         *
         * @return 结果
         */
        public String list() {
            return "ok";
        }
    }

    /** 写数据源配置。 */
    static class WriteService {

        /**
         * 保存。
         *
         * @return 结果
         */
        @DataSource
        public String save() {
            return "ok";
        }
    }
}
