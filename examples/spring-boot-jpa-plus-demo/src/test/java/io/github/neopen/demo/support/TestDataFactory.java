package io.github.neopen.demo.support;

import io.github.neopen.demo.bean.param.OrderParam;
import io.github.neopen.demo.bean.param.OrderProductParam;
import io.github.neopen.demo.bean.param.ProductParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * 测试数据构造工具。
 */
public final class TestDataFactory {

    private TestDataFactory() {
    }

    /**
     * 创建订单新增参数，默认包含两个订单项。
     *
     * @param orderNo 订单编号
     * @param status  订单状态
     * @return 订单参数
     */
    public static OrderParam orderParam(String orderNo, String status) {
        OrderParam param = new OrderParam();
        param.setOrderNo(orderNo);
        param.setStatus(status);
        param.setTotalAmount(new BigDecimal("199.00"));
        param.setUserId(1L);
        param.setReceiverName("测试收货人");
        param.setReceiverAddress("上海市浦东新区");
        param.setReceiverPhone("13800000000");
        param.setSenderName("测试发货人");
        param.setSenderPhone("13900000000");
        param.setShippingAddress("杭州市余杭区");
        param.setRemark("测试订单");
        param.setShippingFee(new BigDecimal("10.00"));
        param.setDiscountAmount(BigDecimal.ZERO);
        param.setPaymentMethod("ALIPAY");
        param.setTrackingNumber("TRACK-" + orderNo);
        param.setOrderProducts(List.of(
                new OrderProductParam(1L, "机械键盘", new BigDecimal("99.00"), 1, "P-001"),
                new OrderProductParam(2L, "无线鼠标", new BigDecimal("100.00"), 1, "P-002")
        ));

        return param;
    }

    /**
     * 创建商品参数。
     *
     * @param productName 商品名称
     * @param stock       库存数量
     * @param safety      安全库存
     * @param type        产品类型
     * @param brand       产品品牌
     * @return 商品参数
     */
    public static ProductParam productParam(
            String productName,
            int stock,
            int safety,
            String type,
            String brand
    ) {
        ProductParam param = new ProductParam();
        param.setProductName(productName);
        param.setUnitPrice(new BigDecimal("59.90"));
        param.setStockQuantity(stock);
        param.setSafetyStock(safety);
        param.setProductCode("CODE-" + productName);
        param.setSkuCode("SKU-" + productName);
        param.setProductType(type);
        param.setProductBrand(brand);
        param.setProductSpec("标准规格");
        param.setManufacturer("示例厂商");
        param.setProductDescription("示例商品");
        param.setWeight(new BigDecimal("1.20"));
        param.setVolume(new BigDecimal("0.50"));
        param.setActive(Boolean.TRUE);

        return param;
    }
}
