# 示例项目

`examples/spring-boot-jpa-plus-demo` 是一个可运行的示例应用，使用 H2 内存数据库，覆盖订单聚合与商品库存两个场景。

## 覆盖内容

- **订单聚合**：JPA CRUD、订单明细批量加载（避免 N+1）、同一查询的 HQL 与原生 SQL 实现。
- **连表与过滤**：`OrderJpaServiceImpl.buildPredicates` 用 `SpecWrapper` 连 `orderProducts`，按明细名称与数量过滤，自动 `distinct` 保证分页计数正确，并支持发件人手机号与订单金额区间。
- **商品库存**：`SpecWrapper` 查询与 HQL、原生 SQL 的对照实现，原生字段比较、批量补货、基于 `SqlWrapper` 的聚合统计。
- **同一接口三种实现**：`OrderService` 与 `ProductService` 各有 JPA、HQL、原生 SQL 三套实现。

## 路由划分

| 前缀 | 实现 |
| --- | --- |
| `/order`、`/product` | JPA |
| `/order/hql`、`/product/hql` | HQL |
| `/order/sql`、`/product/sql` | 原生 SQL（`SqlWrapper` + `JpaPlusSqlExecutor`） |

主要端点：

| 方法 | 路径 | 实现 |
| --- | --- | --- |
| POST | `/order` | JPA |
| GET | `/order/page`、`/order/list`、`/order/{orderNo}` | JPA |
| GET | `/order/hql/page`、`/order/hql/list`、`/order/hql/{orderNo}` | HQL |
| GET | `/order/sql/page`、`/order/sql/list`、`/order/sql/{orderNo}` | 原生 SQL |
| POST | `/product` | JPA |
| GET | `/product/page`、`/product/list`、`/product/{id}` | JPA |
| GET | `/product/low-stock`、`/product/stock-summary` | JPA |
| POST | `/product/restock` | JPA 实体更新 |
| GET | `/product/hql/page`、`/product/hql/list`、`/product/hql/{id}` | HQL |
| GET | `/product/hql/low-stock`、`/product/hql/stock-summary` | HQL + 原生 SQL |
| POST | `/product/hql/restock` | HQL 批量更新 |
| GET | `/product/sql/page`、`/product/sql/list`、`/product/sql/{id}` | 原生 SQL |
| GET | `/product/sql/low-stock`、`/product/sql/stock-summary` | 原生 SQL |
| POST | `/product/sql/restock` | 原生 SQL 批量更新 |

## 运行

```bash
cd examples/spring-boot-jpa-plus-demo
mvn spring-boot:run
```

## 测试

测试按 JUnit `@Tag` 分为三条路线：

```bash
mvn test                       # 全部
mvn test -Punit-test           # 纯单元测试，不启动 Spring 上下文
mvn test -Pintegration-test    # Spring + H2 服务测试，覆盖三套实现
mvn test -Papi-test            # MockMvc HTTP 测试
```

## 使用建议

阅读顺序建议按"同一接口的三套实现"对照进行：先看 JPA 实现理解 `SpecWrapper` 与执行器，再看 HQL 实现理解链式构造与 count 规则，最后看 SQL 实现理解列名转换与结果映射。
