package com.hengline.jpa.bean.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.StringUtils;


/**
 * <b> 分页参数
 * <p>
 * 适用于jpa和mybatis
 *
 * @author He.hp
 * @version 1.0
 * @date 2020年3月16日 下午2:12:27
 */
@ApiModel("分页参数")
public class PageParam extends BaseParam {
    /**
     * 默认排序字段名: id
     */
    public static final String DEFAULT_SORT_FEILD = "id";
    /**
     * 默认每页记录数: 10
     */
    public static final int DEFAULT_ROW = 10;

    @ApiModelProperty("第几页（默认1）")
    @Min(value = 0, message = "页数不能小于1")
    @Max(value = Integer.MAX_VALUE >> 12, message = "页数过大")
    private int start = 1; // 第几页

    @ApiModelProperty("每页大小（默认10）")
    @Min(value = 1, message = "每页大小过小")
    @Max(value = Integer.MAX_VALUE >> 2, message = "每页大小过大")
    private int limit = DEFAULT_ROW; // 每页大小

    @ApiModelProperty("排序字段(如：需要根据name排序时，为该属性传入值name,默认ID)")
    @Size(max = 20, message = "排序字段过大")
    private String sort = DEFAULT_SORT_FEILD; // 排序字段

    @ApiModelProperty("排序顺序(倒序传值0, 顺序传值1, 默认倒序)")
    @Max(value = 1, message = "排序参数错误")
    @Min(value = 0, message = "排序参数错误")
    private int order = 0; // 排序顺序

    public int getStart() {
        return start;
    }

    /**
     * <b> mybatis 的分页起始页参数
     * <p> 经过转换，适用于 mybatis分页， 使用方式，如：LIMIT #{start }, #{limit }
     *
     * @return int
     * @Author He.hp
     * @Date 2021/3/23 15:50
     */
    public int myStart() {
        return start <= 0 ? 0 : (start - 1) * getLimit();
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getLimit() {
        return limit < 5 ? DEFAULT_ROW : limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String getSort() {
        return StringUtils.isEmpty(sort) ? DEFAULT_SORT_FEILD : sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    /**
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * @return
     * @author he.hp
     * @time 2019年1月15日 上午10:25:38
     * @do >>> jpa排序顺序
     */
    @ApiModelProperty(hidden = true)
    private Direction orderDirection() {
        return getOrder() > 0 ? Direction.ASC : Direction.DESC;
    }

    /**
     * @return
     * @author he.hp
     * @time 2019年1月15日 上午10:25:42
     * @do >>> jpa排序字段
     */
    @ApiModelProperty(hidden = true)
    private Sort orderSort() {
        return Sort.by(orderDirection(), getSort());
    }

    /**
     * <p>
     * 获得 Pageable 的实现类对象 PageRequest
     * </p> 默认id倒序排序
     *
     * @return
     * @throws Exception
     */
    public PageRequest pageRequest() throws Exception {
        return pageRequest(orderSort());
    }

    /**
     * <b> 获得分页参数 PageRequest
     * <p> 排序，如：Sort.by(Direction.DESC, "updateTime")
     *
     * @param sort
     * @return
     * @throws Exception
     * @author He.hp
     * @date 2019年7月4日 上午11:22:48
     */
    public PageRequest pageRequest(Sort sort) throws Exception {

        return PageRequest.of(getStart() > 0 ? (getStart() - 1) : 0, getLimit(), (sort == null) ? orderSort() : sort);
    }

    /**
     * <b> 获得分页PageRequest，不排序
     * <p>
     *
     * @return org.springframework.data.domain.PageRequest
     * @Author He.hp
     * @Date 2021/3/23 15:57
     */
    public PageRequest defaultPage() throws Exception {

        return PageRequest.of(getStart() > 0 ? (getStart() - 1) : 0, getLimit());
    }
}
