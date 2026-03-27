package com.hengline.jpa.bean.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 订单状态枚举类
 * 定义订单的所有可能状态
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
    
    private final String code;
    private final String desc;
    private static final List<OrderStatusEnum> VALUES = Arrays.asList(values());
    private static final int SIZE = VALUES.size();
    private static final Random RANDOM = new Random();
    
    /**
     * 构造函数
     * @param code 状态码
     * @param desc 状态描述
     */
    OrderStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    /**
     * 根据状态码获取枚举实例
     * @param code 状态码
     * @return 枚举实例
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
     * 获取所有订单状态列表
     * @return 状态列表
     */
    public static List<OrderStatusEnum> getAllStatus() {
        return VALUES;
    }
    
    /**
     * 随机获取一个订单状态
     * @return 随机订单状态
     */
    public static OrderStatusEnum getRandomStatus() {
        return VALUES.get(RANDOM.nextInt(SIZE));
    }
    
    /**
     * 获取订单的下一个状态（简单的状态流转逻辑）
     * @return 下一个状态，如果已是最终状态则返回null
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
     * 判断订单是否为最终状态
     * @return 是否为最终状态
     */
    public boolean isFinalStatus() {
        return this == DONE || this == CANCEL;
    }
}