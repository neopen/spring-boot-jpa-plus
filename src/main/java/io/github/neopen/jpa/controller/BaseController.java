package io.github.neopen.jpa.controller;

import io.github.neopen.jpa.annotation.SysControllerLog;
import io.github.neopen.jpa.bean.Json;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * <b> 通用查询接口
 * <p> Q为查询参数对象， V为返回对象
 *
 * @Author He.hp
 * @Date 2021/4/13 15:06
 */
public interface BaseController<Q, V> {

    /**
     * <b> 获得分页
     * <p>
     *
     * @param param
     * @return com.citycloud.sanitation.common.bean.model.Json<java.util.List < V>>
     * @Author He.hp
     * @Date 2021/4/13 15:10
     */
    @GetMapping("/page")
    @ApiOperation("分页列表")
    @SysControllerLog("分页列表")
    default Json<List<V>> getPage(Q param) throws Exception {
        return Json.getError();
    }

    /**
     * <b> 获得所有
     * <p>
     *
     * @param param
     * @return com.citycloud.sanitation.common.bean.model.Json<java.util.List < V>>
     * @Author He.hp
     * @Date 2021/4/13 15:10
     */
    @GetMapping("/list")
    @ApiOperation("所有列表")
    @SysControllerLog("所有列表")
    default Json<List<V>> getList(@Nullable Q param) throws Exception {
        return Json.getError();
    }

    /**
     * <b> 详情
     * <p>
     *
     * @param sId
     * @return com.citycloud.sanitation.common.bean.model.Json<V>
     * @Author He.hp
     * @Date 2021/4/13 15:11
     */
    @GetMapping("/detail")
    @ApiOperation("详情")
    @SysControllerLog("详情")
    default <S extends V> Json<S> getDetail(@ApiParam("id") @RequestParam("id") Object sId) throws Exception {
        return Json.getError();
    }


}
