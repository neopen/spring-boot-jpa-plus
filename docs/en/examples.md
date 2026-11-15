# Example Project

`examples/spring-boot-jpa-plus-demo` is a runnable example application that uses an H2 in-memory database and covers two scenarios: order aggregation and product stock.

## What it covers

- **Order aggregation**: JPA CRUD, batch loading of order items (avoiding N+1), and HQL plus native SQL implementations of the same query.
- **Joins and filtering**: `OrderJpaServiceImpl.buildPredicates` uses `SpecWrapper` to join `orderProducts`, filters by item name and quantity, enables `distinct` automatically so that pagination counting stays correct, and supports the sender phone number and order amount range.
- **Product stock**: a `SpecWrapper` query compared with HQL and native SQL implementations, native field comparisons, batch restocking, and aggregate statistics based on `SqlWrapper`.
- **Three implementations of the same interface**: `OrderService` and `ProductService` each have JPA, HQL and native SQL implementations.

## Route layout

| Prefix | Implementation |
| --- | --- |
| `/order`, `/product` | JPA |
| `/order/hql`, `/product/hql` | HQL |
| `/order/sql`, `/product/sql` | Native SQL (`SqlWrapper` + `JpaPlusSqlExecutor`) |

Main endpoints:

| Method | Path | Implementation |
| --- | --- | --- |
| POST | `/order` | JPA |
| GET | `/order/page`, `/order/list`, `/order/{orderNo}` | JPA |
| GET | `/order/hql/page`, `/order/hql/list`, `/order/hql/{orderNo}` | HQL |
| GET | `/order/sql/page`, `/order/sql/list`, `/order/sql/{orderNo}` | Native SQL |
| POST | `/product` | JPA |
| GET | `/product/page`, `/product/list`, `/product/{id}` | JPA |
| GET | `/product/low-stock`, `/product/stock-summary` | JPA |
| POST | `/product/restock` | JPA entity update |
| GET | `/product/hql/page`, `/product/hql/list`, `/product/hql/{id}` | HQL |
| GET | `/product/hql/low-stock`, `/product/hql/stock-summary` | HQL + native SQL |
| POST | `/product/hql/restock` | HQL batch update |
| GET | `/product/sql/page`, `/product/sql/list`, `/product/sql/{id}` | Native SQL |
| GET | `/product/sql/low-stock`, `/product/sql/stock-summary` | Native SQL |
| POST | `/product/sql/restock` | Native SQL batch update |

## Running

```bash
cd examples/spring-boot-jpa-plus-demo
mvn spring-boot:run
```

## Testing

The tests are split into three tracks by JUnit `@Tag`:

```bash
mvn test                       # 全部
mvn test -Punit-test           # 纯单元测试，不启动 Spring 上下文
mvn test -Pintegration-test    # Spring + H2 服务测试，覆盖三套实现
mvn test -Papi-test            # MockMvc HTTP 测试
```

## Recommended reading

It is recommended to read the code by comparing the three implementations of the same interface: first the JPA implementation to understand `SpecWrapper` and the executor, then the HQL implementation to understand the fluent construction and the count rules, and finally the SQL implementation to understand column name conversion and result mapping.
