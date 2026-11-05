package io.github.neopen.demo.bean.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 订单状态枚举。
 *
 * <p>定义示例订单的状态编码、描述及简单流转规则。</p>
 */
@Getter
public enum OrderStatusEnum {
    
    /**
     * 待付款
     */
    PENDING("PENDING", "待付款"),
    
    /**
     * 已付款
     */
    PAID("PAID", "已付款"),
    
    /**
     * 已发货
     */
    SHIPPED("SHIPPED", "已发货"),
    
    /**
     * 已完成
     */
    DONE("DONE", "已完成"),
    
    /**
     * 已取消
     */
    CANCEL("CANCEL", "已取消");
    
    /** 状态编码。 */
    private final String code;

    /** 状态中文描述。 */
    private final String desc;

    /** 全部订单状态。 */
    private static final List<OrderStatusEnum> VALUES = Arrays.asList(values());

    /** 订单状态数量。 */
    private static final int SIZE = VALUES.size();

    /** 随机状态生成器。 */
    private static final Random RANDOM = new Random();
    
    /**
     * 创建订单状态。
     *
     * @param code 状态码
     * @param desc 状态描述
     */
    OrderStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    /**
     * 根据状态码获取枚举实例。
     *
     * @param code 状态码
     * @return 匹配的枚举实例；未匹配时返回 {@code null}
     */
    public static OrderStatusEnum getByCode(String code) {
        for (OrderStatusEnum status : VALUES) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * 获取所有订单状态。
     *
     * @return 状态列表
     */
    public static List<OrderStatusEnum> getAllStatus() {
        return VALUES;
    }
    
    /**
     * 随机获取一个订单状态。
     *
     * @return 随机订单状态
     */
    public static OrderStatusEnum getRandomStatus() {
        return VALUES.get(RANDOM.nextInt(SIZE));
    }
    
    /**
     * 获取简单流转规则中的下一个状态。
     *
     * @return 下一个状态；已完成或已取消时返回 {@code null}
     */
    public OrderStatusEnum getNextStatus() {
        switch (this) {
            case PENDING:
                return PAID;
            case PAID:
                return SHIPPED;
            case SHIPPED:
                return DONE;
            default:
                // DONE 和 CANCEL 是最终状态
                return null;
        }
    }
    
    /**
     * 判断订单是否为最终状态。
     *
     * @return 已完成或已取消时返回 {@code true}
     */
    public boolean isFinalStatus() {
        return this == DONE || this == CANCEL;
    }
}