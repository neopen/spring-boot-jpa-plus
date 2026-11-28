package io.github.neopen.demo.controller;

import io.github.neopen.convert.annotation.DateStrValid;
import io.github.neopen.core.exception.JpaPlusException;
import io.github.neopen.demo.bean.param.ConvertOrderParam;
import io.github.neopen.demo.bean.vo.ConvertOrderPreviewVo;
import io.github.neopen.demo.service.ConvertDemoService;
import io.github.neopen.extension.annotation.SysControllerLog;
import io.github.neopen.extension.response.Json;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 参数映射示例接口。
 *
 * <p>通过不落库的预览接口演示 {@code ParamMapper} 与
 * {@code ParamMapping} 的主要能力：</p>
 *
 * <ul>
 *     <li>{@code POST /convert/order/preview}：列名别名、嵌套路径、
 *         外键引用、字段重命名、自定义转换、JPA 托管字段跳过；</li>
 *     <li>{@code POST /convert/order/strict}：严格模式拒绝未匹配字段；</li>
 *     <li>{@code POST /convert/order/null-policy}：对比 IGNORE 与
 *         SET_NULL 两种空值策略；</li>
 *     <li>{@code POST /convert/order/validate}：日期字符串校验。</li>
 * </ul>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@RestController
@RequestMapping("/convert")
public class ConvertDemoController {

    /** 参数映射示例服务。 */
    private final ConvertDemoService convertDemoService;

    /**
     * 创建参数映射示例控制器。
     *
     * @param convertDemoService 参数映射示例服务
     */
    public ConvertDemoController(
            ConvertDemoService convertDemoService
    ) {
        this.convertDemoService = convertDemoService;
    }

    /**
     * 参数到实体映射预览。
     *
     * @param param 参数
     * @return 映射预览
     */
    @PostMapping("/order/preview")
    @DateStrValid
    @ApiOperation("参数映射预览")
    @SysControllerLog("参数映射预览")
    public Json<ConvertOrderPreviewVo> preview(
            @RequestBody ConvertOrderParam param
    ) {
        return Json.getSucc(convertDemoService.preview(param));
    }

    /**
     * 严格模式映射。
     *
     * <p>参数类声明了 {@code unknownField}，严格模式会返回错误信息。</p>
     *
     * @param param 参数
     * @return 成功时返回映射预览，失败时返回错误信息
     */
    @PostMapping("/order/strict")
    @ApiOperation("严格模式映射")
    @SysControllerLog("严格模式映射")
    public Json<ConvertOrderPreviewVo> strict(
            @RequestBody ConvertOrderParam param
    ) {
        try {
            return Json.getSucc(convertDemoService.strict(param));
        } catch (JpaPlusException ex) {
            return Json.getError(ex.getMessage());
        }
    }

    /**
     * 空值策略对比。
     *
     * @return 默认策略与 SET_NULL 策略的结果
     */
    @PostMapping("/order/null-policy")
    @ApiOperation("空值策略对比")
    @SysControllerLog("空值策略对比")
    public Json<Map<String, Object>> nullPolicy() {
        return Json.getSucc(convertDemoService.nullPolicyDemo());
    }

    /**
     * 日期字符串校验。
     *
     * <p>{@code @DateStrValid} 触发切面，校验参数中
     * {@code @DateStrFormat} 标注的字段。</p>
     *
     * @param param 参数
     * @return 校验通过提示
     */
    @PostMapping("/order/validate")
    @DateStrValid
    @ApiOperation("日期字符串校验")
    @SysControllerLog("日期字符串校验")
    public Json<String> validate(
            @RequestBody ConvertOrderParam param
    ) {
        return Json.getSucc("日期校验通过");
    }
}
