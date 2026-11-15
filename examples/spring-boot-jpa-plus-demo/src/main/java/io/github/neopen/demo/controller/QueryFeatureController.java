package io.github.neopen.demo.controller;

import io.github.neopen.demo.bean.entity.CustomerSecret;
import io.github.neopen.demo.bean.vo.OrderSummaryVo;
import io.github.neopen.demo.service.CustomerSecretService;
import io.github.neopen.demo.service.QueryFeatureService;
import io.github.neopen.extension.annotation.SysControllerLog;
import io.github.neopen.extension.response.Json;
import io.swagger.annotations.ApiOperation;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 0.5 新增能力示例接口。
 *
 * <p>覆盖请求分页参数、union 合并、数据源作用域与字段加密，
 * 便于直接调用验证文档中的用法。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@RestController
@RequestMapping("/feature")
public class QueryFeatureController {

    /** 查询能力示例服务。 */
    private final QueryFeatureService queryFeatureService;

    /** 客户敏感信息服务。 */
    private final CustomerSecretService customerSecretService;

    /**
     * 创建示例控制器。
     *
     * @param queryFeatureService    查询能力示例服务
     * @param customerSecretService  客户敏感信息服务
     */
    public QueryFeatureController(
            QueryFeatureService queryFeatureService,
            CustomerSecretService customerSecretService
    ) {
        this.queryFeatureService = queryFeatureService;
        this.customerSecretService = customerSecretService;
    }

    /**
     * 使用 {@link io.github.neopen.core.persistence.page.PageParams}
     * 分页查询订单。
     *
     * @param page    页码，从 1 开始
     * @param size    每页数量
     * @param sort    排序表达式，例如 {@code id,desc}
     * @param orderNo 订单编号关键字
     * @return 订单分页响应
     */
    @GetMapping("/page")
    @ApiOperation("PageParams 分页查询")
    @SysControllerLog("PageParams 分页查询")
    public Json<List<OrderSummaryVo>> getPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String orderNo
    ) {
        Page<OrderSummaryVo> result = queryFeatureService.getPage(
                page,
                size,
                sort,
                orderNo
        );

        return Json.getSucc(result.getContent(), result.getTotalElements());
    }

    /**
     * 使用 union 合并已支付与已取消订单。
     *
     * @return 订单摘要列表
     */
    @GetMapping("/union")
    @ApiOperation("union 合并查询")
    @SysControllerLog("union 合并查询")
    public Json<List<OrderSummaryVo>> getUnion() {
        return Json.getSuccs(queryFeatureService.getPaidAndCancelled());
    }

    /**
     * 在归档作用域内查询订单，包含已逻辑删除的数据。
     *
     * @param orderNo 订单编号关键字
     * @return 订单摘要列表
     */
    @GetMapping("/scope")
    @ApiOperation("数据源作用域查询")
    @SysControllerLog("数据源作用域查询")
    public Json<List<OrderSummaryVo>> getScopedList(
            @RequestParam(required = false) String orderNo
    ) {
        return Json.getSuccs(
                queryFeatureService.listIncludingDeleted(orderNo)
        );
    }

    /**
     * 保存客户敏感信息，演示字段加密写入。
     *
     * @param customerNo 客户编号
     * @param idCard     身份证号明文
     * @param phone      手机号明文
     * @return 保存后的客户信息
     */
    @PostMapping("/secret")
    @ApiOperation("加密字段写入")
    @SysControllerLog("加密字段写入")
    public Json<CustomerSecret> saveSecret(
            @RequestParam String customerNo,
            @RequestParam String idCard,
            @RequestParam String phone
    ) {
        return Json.getSucc(
                customerSecretService.save(customerNo, idCard, phone)
        );
    }

    /**
     * 按手机号查询客户，演示确定性加密的等值查询。
     *
     * @param phone 手机号明文
     * @return 匹配的客户信息列表
     */
    @GetMapping("/secret")
    @ApiOperation("确定性加密查询")
    @SysControllerLog("确定性加密查询")
    public Json<List<CustomerSecret>> getSecretByPhone(
            @RequestParam String phone
    ) {
        return Json.getSuccs(customerSecretService.findByPhone(phone));
    }
}
