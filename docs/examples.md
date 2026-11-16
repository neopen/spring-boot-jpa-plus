# 示例项目

`examples/spring-boot-jpa-plus-demo` 是一个可运行的示例应用，使用 H2 内存数据库，覆盖订单聚合与商品库存两个场景。

## 覆盖内容

- **订单聚合**：JPA CRUD、订单明细批量加载（避免 N+1）、同一查询的 HQL 与原生 SQL 实现。
- **连表与过滤**：`OrderJpaServiceImpl.buildPredicates` 用 `SpecWrapper` 连 `orderProducts`，按明细名称与数量过滤，自动 `distinct` 保证分页计数正确，并支持发件人手机号与订单金额区间。
- **商品库存**：`SpecWrapper` 查询与 HQL、原生 SQL 的对照实现，原生字段比较、批量补货、基于 `SqlWrapper` 的聚合统计。
- **同一接口三种实现**：`OrderService` 与 `ProductService` 各有 JPA、HQL、原生 SQL 三套实现。
- **0.5 新增能力**：`QueryFeatureService`、`CustomerSecretService` 与 `AuditConfiguration` 演示请求分页、union、数据源作用域、字段加密与审计操作人。
- **0.6 新增能力**：`DocumentFeatureService` 与 `FeatureDataConfiguration` 演示多租户、行级数据权限、逻辑删除恢复、变更历史与批量 upsert。

## 0.6 新能力示例

`DocumentFeatureController` 演示 0.6 新增能力，实体与依赖见 `bean/entity/DocumentRecord`、`config/FeatureDataConfiguration`：

| 方法 | 路径 | 演示内容 |
| --- | --- | --- |
| GET | `/feature/documents` | 自动追加租户与部门权限条件 |
| POST | `/feature/documents?docNo=DOC-001&title=...` | 写入时自动填充租户字段 |
| POST | `/feature/documents/{id}/rename?title=...` | 生成 `UPDATE` 变更历史 |
| DELETE | `/feature/documents/{id}` | 逻辑删除 |
| POST | `/feature/documents/{id}/restore` | `LogicDeletes.restoreById` 恢复逻辑删除 |
| POST | `/feature/documents/upsert` | `BatchOperation.upsertAll` 按 `docNo` 新增或更新 |
| GET | `/feature/documents/audit` | 查看当前进程内的变更历史 |

请求头 `X-Demo-Tenant` 与 `X-Demo-Dept` 分别指定租户与部门，未提供时使用 `demo-tenant` 与默认部门：

```bash
curl -H "X-Demo-Tenant: tenant-a" -H "X-Demo-Dept: 10" \
  "http://localhost:8080/feature/documents"
```

- `FeatureDataConfiguration` 注册 `TenantProvider`、`DataPermissionProvider` 与 `AuditLogPublisher`。
- `DocumentRecord` 同时标注 `@Audited`、`@TenantId`、`@LogicDelete`，并在 `@EntityListeners` 中注册两个监听器。
- `DemoAuditStore` 用内存列表保存变更历史，示例重启后清空。

## 0.5 新能力示例

`QueryFeatureController` 集中演示 0.5 新增能力，代码见 `service/QueryFeatureService`、`service/CustomerSecretService`：

| 方法 | 路径 | 演示内容 |
| --- | --- | --- |
| GET | `/feature/page?page=1&size=10&sort=id,desc` | `PageParams` 校验页码、每页数量与排序字段 |
| GET | `/feature/union` | `SqlWrapper.union` 合并已支付与已取消订单 |
| GET | `/feature/scope?orderNo=SD-` | `JpaPlusScope` 绑定 `archive-view` 作用域后读取已逻辑删除订单 |
| POST | `/feature/secret?customerNo=C-001&idCard=...&phone=...` | 随机 IV 与确定性加密字段写入 |
| GET | `/feature/secret?phone=13800001234` | 按确定性密文等值查询手机号 |

对应配置位于 `application.yml`：

```yaml
neopen:
  jpa:
    encryption:
      key: demo-encryption-key
    scopes:
      archive-view:
        logic-delete-enabled: false
```

- `archive-view` 作用域只关闭逻辑删除过滤，其他查询仍然过滤已删除数据。
- 手机号使用 `DeterministicEncryptedFieldConverter`，查询前把明文加密为密文即可匹配；身份证号使用随机 IV，相同明文每次密文不同。
- `AuditConfiguration` 注册 `AuditorProvider`，优先读取 `X-Demo-User` 请求头，非 Web 场景回退为 `demo-user`。

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
