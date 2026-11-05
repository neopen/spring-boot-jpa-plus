package io.github.neopen.demo.bean.param;

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
 * 通用分页参数。
 *
 * <p>页码从 1 开始，可转换为 Spring Data 分页请求，也支持计算
 * MyBatis 查询偏移量。</p>
 */
@ApiModel("分页参数")
public class PageParam extends BaseParam {
    /** 默认排序字段名。 */
    public static final String DEFAULT_SORT_FEILD = "id";
    /** 默认每页记录数。 */
    public static final int DEFAULT_ROW = 10;

    /** 页码，从 1 开始。 */
    @ApiModelProperty("第几页（默认1）")
    @Min(value = 0, message = "页数不能小于1")
    @Max(value = Integer.MAX_VALUE >> 12, message = "页数过大")
    private int start = 1;

    /** 每页记录数。 */
    @ApiModelProperty("每页大小（默认10）")
    @Min(value = 1, message = "每页大小过小")
    @Max(value = Integer.MAX_VALUE >> 2, message = "每页大小过大")
    private int limit = DEFAULT_ROW;

    /** 排序字段名。 */
    @ApiModelProperty("排序字段(如：需要根据name排序时，为该属性传入值name,默认ID)")
    @Size(max = 20, message = "排序字段过大")
    private String sort = DEFAULT_SORT_FEILD;

    /** 排序方向：0 表示降序，1 表示升序。 */
    @ApiModelProperty("排序顺序(倒序传值0, 顺序传值1, 默认倒序)")
    @Max(value = 1, message = "排序参数错误")
    @Min(value = 0, message = "排序参数错误")
    private int order = 0;

    /**
     * 获取页码。
     *
     * @return 从 1 开始的页码
     */
    public int getStart() {
        return start;
    }

    /**
     * 计算 MyBatis 分页查询的记录偏移量。
     *
     * @return 从 0 开始的记录偏移量
     */
    public int myStart() {
        return start <= 0 ? 0 : (start - 1) * getLimit();
    }

    /**
     * 设置页码。
     *
     * @param start 从 1 开始的页码
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * 获取每页记录数。
     *
     * @return 每页记录数；原始值小于 5 时返回默认值
     */
    public int getLimit() {
        return limit < 5 ? DEFAULT_ROW : limit;
    }

    /**
     * 设置每页记录数。
     *
     * @param limit 每页记录数
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * 获取排序字段。
     *
     * @return 排序字段；未设置时返回默认字段
     */
    public String getSort() {
        return StringUtils.isEmpty(sort) ? DEFAULT_SORT_FEILD : sort;
    }

    /**
     * 设置排序字段。
     *
     * @param sort 排序字段名
     */
    public void setSort(String sort) {
        this.sort = sort;
    }

    /**
     * 获取排序方向参数。
     *
     * @return 0 表示降序，1 表示升序
     */
    public int getOrder() {
        return order;
    }

    /**
     * 设置排序方向参数。
     *
     * @param order 0 表示降序，1 表示升序
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * 将排序方向参数转换为 Spring Data 枚举。
     *
     * @return 排序方向
     */
    @ApiModelProperty(hidden = true)
    private Direction orderDirection() {
        return getOrder() > 0 ? Direction.ASC : Direction.DESC;
    }

    /**
     * 构建 Spring Data 排序规则。
     *
     * @return 排序规则
     */
    @ApiModelProperty(hidden = true)
    private Sort orderSort() {
        return Sort.by(orderDirection(), getSort());
    }

    /**
     * 创建使用默认排序规则的分页请求。
     *
     * @return 分页请求
     * @throws Exception 创建分页请求失败时抛出
     */
    public PageRequest pageRequest() throws Exception {
        return pageRequest(orderSort());
    }

    /**
     * 创建使用指定排序规则的分页请求。
     *
     * @param sort 排序规则；为 {@code null} 时使用默认排序
     * @return 分页请求
     * @throws Exception 创建分页请求失败时抛出
     */
    public PageRequest pageRequest(Sort sort) throws Exception {

        return PageRequest.of(getStart() > 0 ? (getStart() - 1) : 0, getLimit(), (sort == null) ? orderSort() : sort);
    }

    /**
     * 创建不包含排序规则的分页请求。
     *
     * @return 未排序的分页请求
     * @throws Exception 创建分页请求失败时抛出
     */
    public PageRequest defaultPage() throws Exception {

        return PageRequest.of(getStart() > 0 ? (getStart() - 1) : 0, getLimit());
    }
}
