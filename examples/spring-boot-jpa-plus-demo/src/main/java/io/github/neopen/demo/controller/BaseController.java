package io.github.neopen.demo.controller;

import io.github.neopen.extension.annotation.SysControllerLog;
import io.github.neopen.extension.response.Json;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 通用查询控制器接口。
 *
 * @param <Q> 查询参数类型
 * @param <V> 响应视图对象类型
 */
public interface BaseController<Q, V> {

    /**
     * 获取分页列表。
     *
     * <p>默认返回失败响应，具体控制器应重写此方法。</p>
     *
     * @param param 查询参数
     * @return 分页响应
     * @throws Exception 查询失败时抛出
     */
    @GetMapping("/page")
    @ApiOperation("分页列表")
    @SysControllerLog("分页列表")
    default Json<List<V>> getPage(Q param) throws Exception {
        return Json.getError();
    }

    /**
     * 获取全部列表。
     *
     * <p>默认返回失败响应，具体控制器应重写此方法。</p>
     *
     * @param param 查询参数，允许为 {@code null}
     * @return 列表响应
     * @throws Exception 查询失败时抛出
     */
    @GetMapping("/list")
    @ApiOperation("所有列表")
    @SysControllerLog("所有列表")
    default Json<List<V>> getList(@Nullable Q param) throws Exception {
        return Json.getError();
    }

    /**
     * 获取详情。
     *
     * <p>默认返回失败响应，具体控制器应重写此方法。</p>
     *
     * @param <S> 具体视图对象类型
     * @param id 查询标识
     * @return 详情响应
     * @throws Exception 查询失败时抛出
     */
    @GetMapping("/{id}")
    @ApiOperation("详情")
    @SysControllerLog("详情")
    default <S extends V> Json<S> getDetail(@ApiParam("id") @PathVariable("id") Object id) throws Exception {
        return Json.getError();
    }


}
