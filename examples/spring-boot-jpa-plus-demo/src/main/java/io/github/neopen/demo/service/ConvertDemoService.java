package io.github.neopen.demo.service;

import io.github.neopen.convert.mapping.MappingResult;
import io.github.neopen.convert.mapping.NullPolicy;
import io.github.neopen.convert.mapping.ParamMapper;
import io.github.neopen.convert.mapping.ParamMapping;
import io.github.neopen.demo.bean.entity.ConvertAddress;
import io.github.neopen.demo.bean.entity.ConvertOrder;
import io.github.neopen.demo.bean.param.ConvertOrderParam;
import io.github.neopen.demo.bean.vo.ConvertOrderPreviewVo;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 参数映射示例服务。
 *
 * <p>不落库，仅把参数映射到实体并返回预览结果，方便直接观察字段匹配、
 * 类型转换、关联引用和 JPA 托管字段跳过等行为。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Service
public class ConvertDemoService {

    /** 实体管理器，用于解析 {@code @ParamReference}。 */
    private final EntityManager entityManager;

    /**
     * 创建参数映射示例服务。
     *
     * @param entityManager 实体管理器
     */
    public ConvertDemoService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 按默认规则映射参数并返回预览。
     *
     * @param param 参数
     * @return 映射预览
     */
    public ConvertOrderPreviewVo preview(ConvertOrderParam param) {
        return toVo(map(param, false));
    }

    /**
     * 按严格模式映射参数。
     *
     * <p>参数中声明了实体不存在的 {@code unknownField}，因此严格模式
     * 会抛出异常，用于演示字段拼写错误的快速失败。</p>
     *
     * @param param 参数
     * @return 映射预览
     */
    public ConvertOrderPreviewVo strict(ConvertOrderParam param) {
        return toVo(map(param, true));
    }

    /**
     * 演示 {@link NullPolicy} 对空字段的处理差异。
     *
     * @return 默认策略与 SET_NULL 策略的结果
     */
    public Map<String, Object> nullPolicyDemo() {
        ConvertOrder order = new ConvertOrder();
        order.setRemark("旧备注");

        ParamMapping<ConvertOrderParam, ConvertOrder> mapping =
                ParamMapper.mapper(
                                ConvertOrderParam.class,
                                ConvertOrder.class
                        )
                        .ignoreJpaManaged(true)
                        .rename("memo", "remark");

        mapping.apply(new ConvertOrderParam(), order);
        String afterIgnore = order.getRemark();

        mapping.nullPolicy(NullPolicy.SET_NULL)
                .apply(new ConvertOrderParam(), order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("afterIgnore", afterIgnore);
        result.put("afterSetNull", order.getRemark());

        return result;
    }

    /**
     * 执行参数到实体的映射。
     *
     * @param param  参数
     * @param strict 是否启用严格模式
     * @return 映射结果
     */
    private MappingResult<ConvertOrder> map(
            ConvertOrderParam param,
            boolean strict
    ) {
        ConvertOrder order = new ConvertOrder();
        order.setId(1000L);
        order.setVersion(9L);
        order.setCreateTime(LocalDateTime.of(2020, 1, 1, 0, 0));
        order.setUpdateTime(LocalDateTime.of(2020, 1, 2, 0, 0));
        order.setRemark("默认备注");

        return ParamMapper.mapper(ConvertOrderParam.class, ConvertOrder.class)
                .ignoreJpaManaged(true)
                .strict(strict)
                .rename("memo", "remark")
                .convert("memo", (value, context) -> "[" + value + "]")
                .map(param, order, entityManager);
    }

    /**
     * 把映射结果转换为预览对象。
     *
     * @param result 映射结果
     * @return 预览对象
     */
    private ConvertOrderPreviewVo toVo(
            MappingResult<ConvertOrder> result
    ) {
        ConvertOrder entity = result.entity();
        ConvertOrderPreviewVo vo = new ConvertOrderPreviewVo();

        vo.setId(entity.getId());
        vo.setVersion(entity.getVersion());
        vo.setOrderNo(entity.getNumber());
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setOrderDate(entity.getOrderDate());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setProductReferenceResolved(entity.getProduct() != null);

        ConvertAddress address = entity.getAddress();

        if (address != null) {
            vo.setCity(address.getCity());
            vo.setDetail(address.getDetail());
        }

        vo.setUnknownFields(result.unknownFields());
        vo.setConvertedFields(result.convertedFields());

        return vo;
    }
}
