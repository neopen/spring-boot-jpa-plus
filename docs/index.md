---
layout: home

hero:
  name: Spring Boot JPA Plus
  text: Spring Boot 的 JPA 增强工具包
  tagline: 不替换 JPA，只补齐动态查询、HQL / 原生 SQL 构建、实体转换、批量与分页中重复的手写代码。
  actions:
    - theme: brand
      text: 快速开始
      link: /guide/getting-started
    - theme: alt
      text: 核心能力
      link: /core/dao
    - theme: alt
      text: GitHub
      link: https://github.com/neopen/spring-boot-jpa-plus

features:
  - title: 三种查询方式，一套条件模型
    details: "JPA Criteria、HQL、原生 SQL 共用 @QueryCondition 注解条件、安全排序与分页约定，同一份查询参数可以在三种实现间切换。"
  - title: 连表分页不重复计数
    details: "SpecWrapper 在连表时自动追加 distinct；FetchJoinPaging 采用 count → 主键分页 → 主键加载的三段式查询，把分页留在数据库侧。"
  - title: 审计与逻辑删除开箱可用
    details: "@CreatedAt、@UpdatedAt、@CreatedBy、@UpdatedBy 由实体监听器维护；@LogicDelete 统一追加过滤条件，并通过配置项全局开关。"
  - title: 实体与参数的显式映射
    details: "BaseVoService 负责 Entity → VO，ParamMapper 负责 Param → Entity，忽略大小写与下划线，只覆盖非空字段，适合部分更新。"
  - title: 批量与投影
    details: "BatchOperation 按批次 flush / clear；ResultMapper 按别名匹配 DTO，order_no、orderNo、ORDER_NO 都能命中 orderNo。"
  - title: 可选扩展组件
    details: "幂等、限流、统一分页响应各自独立，按配置项启用，不引入时对核心模块零侵入。"
---

::: warning 版本提示
本站文档对应 **0.5 版本**。当前 Maven 正式发布版本为 **0.4.x**，不支持 0.5 新增能力，
详见 [版本发布](/version)。
:::

## 设计边界

Spring Boot JPA Plus 是构建在 Spring Data JPA 之上的增强层，不是新的 ORM：

- 实体、Repository 仍由 Spring Data JPA 管理，`JpaRepository` 与 `JpaSpecificationExecutor` 可以继续使用。
- 逻辑删除过滤只作用于 JPA Criteria 查询，原生 SQL 需要自行处理删除条件。
- 限流与幂等基于本地内存实现，多实例部署需要替换为分布式方案。

## 核心模块

| 模块 | 职责 |
| --- | --- |
| `spring-boot-jpa-plus-core` | 通用 DAO、动态条件、Specification 构建器与执行器、HQL / 原生 SQL 执行器、逻辑删除、审计、批量、投影 |
| `spring-boot-jpa-plus-convert` | Entity → VO 转换服务、Param → Entity 映射 |
| `spring-boot-jpa-plus-autoconfigure` | `neopen.jpa.*` 配置绑定与执行器注册 |
| `spring-boot-jpa-plus-starter` | 统一依赖入口 |
| `spring-boot-jpa-plus-extension` | 幂等、限流、操作日志注解、统一分页响应 |

## 一段代码看清差异

同一个分页查询，用 `SpecWrapper` 表达条件，用执行器完成计数与分页：

```java
JpaPlusSpecExecutor<Order> executor =
        JpaPlusSpecExecutor.of(entityManager, Order.class, "id");

Page<Order> page = executor.findPage(
        SpecWrapper.create(Order.class)
                .like("orderNo", "DS")
                .ge("totalAmount", new BigDecimal("100"))
                .join("orderProducts")
                .like("orderProducts.productName", "机械")
                .filterDeleted()
                .toSpecification(),
        PageRequest.of(0, 10, Sorts.parse("createTime,desc", Order.class))
);
```

连表产生的重复行由 `distinct` 与 `countDistinct` 处理，不需要在业务侧再写一遍计数语句。
