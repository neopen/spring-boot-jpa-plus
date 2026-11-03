# Spring Boot JPA Plus

[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.x-brightgreen)](https://spring.io/projects/spring-boot)
[![JPA](https://img.shields.io/badge/Jakarta%20Persistence-3.x-blue)](https://jakarta.ee/specifications/persistence/)

**Spring Boot JPA Plus** 是一个面向 Spring Boot / Spring Data JPA 的轻量级增强组件。它不替代 Spring Data JPA，也不重新实现 ORM，而是把常用的 `EntityManager` 操作、参数绑定、分页查询和动态条件构建整理成更统一的 API。

> 当前版本处于早期整理阶段（`0.1.0-SNAPSHOT`）。API 尚未承诺稳定，生产使用前请先在目标数据库和事务场景中完成验证。

## 设计目标

- **减少重复代码**：统一 HQL、原生 SQL 的参数绑定、列表查询和分页查询入口。
- **保留 JPA 能力**：底层仍使用 `EntityManager`、Spring Data `Pageable` 和 `Specification`。
- **不接管业务模型**：不要求使用组件自带的 Entity、Repository 或 Controller。
- **按需引入**：核心依赖聚焦 Spring Data JPA；Web、AOP、校验及工具扩展不应成为使用核心 DAO 的前置条件。
- **明确事务边界**：写操作应由调用方的 Spring `@Transactional` 管理，组件不擅自开启或吞掉事务异常。

## 功能一览

| 模块 | 当前提供 | 说明 |
|---|---|---|
| HQL 查询 | `find`、`findAll`、`findPage`、`count`、`executeUpdate` | 支持位置参数和命名参数 |
| Native SQL | `find`、`findAll`、`findPage`、`count`、`executeUpdate` | 支持结果映射类型、位置参数和命名参数 |
| 批量持久化 | HQL DAO `persistAll` | 使用 `EntityManager.persist`，按批次 `flush`，不隐式清空持久化上下文 |
| 动态条件 | `BasePredicateService`、`AbstractBaseService` | 基于 JPA Criteria API / `Specification` |
| 分页参数 | `PageParam` | 转换为 Spring Data `PageRequest` |
| Entity / VO 转换 | `BaseVoService`、`AbstractBaseService` | 提供单条和批量转换扩展点 |
| 自动配置 | `JpaPlusAutoConfiguration` | 自动注册 HQL DAO 与 Native SQL DAO |
| Web 辅助扩展 | 注解、切面、限流/幂等工具 | 保留在 `examples/legacy-web-extension`，不打入核心 Starter |

### 当前版本暂不承诺

- 不提供通用 Repository 替代品；简单 CRUD 优先使用 `JpaRepository`。
- 不自动解析任意 SQL 并生成 count SQL。
- 不保证不同数据库的 Native SQL、分页语法和结果映射完全一致。
- 不提供分布式幂等、分布式限流或跨节点缓存保证。
- 不自动创建事务；批量写入必须在调用方事务中执行。
- 不把任意 SQL 字符串拼接视为安全的参数化查询。

## 环境要求

| 项目 | 要求 |
|---|---|
| Java | 17+ |
| Spring Boot | 3.4.x |
| Spring Data JPA | 由 Spring Boot BOM 管理 |
| Persistence API | Jakarta Persistence（`jakarta.persistence.*`） |
| Build | Maven 3.8+ |

此代码线使用 Jakarta 命名空间，不兼容 Spring Boot 2.x / `javax.persistence.*`。如需支持 Boot 2，应单独维护兼容分支或独立 artifact，不建议在同一个 jar 中混用 `javax` 与 `jakarta`。

## 安装

### Maven

```xml
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

项目发布前，请将版本替换为 Maven Central 中实际发布的版本。当前源码版本为 `0.1.0-SNAPSHOT`，并不代表已经发布。

应用仍需自行引入数据库驱动，并配置数据源：

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

如果使用 MySQL，请替换为对应 JDBC Driver。组件不绑定具体数据库。

### Spring Boot 自动配置

引入依赖并配置好 `spring-boot-starter-data-jpa` 后，Starter 会注册：

- `hqlDao`：HQL 查询实现。
- `sqlDao`：Native SQL 查询实现。

可以直接按具体类型注入，也可以使用 `@Qualifier`：

```java
@Service
public class UserQueryService {

    private final EntityManagerHqlDao hqlDao;
    private final EntityManagerSqlDao sqlDao;

    public UserQueryService(EntityManagerHqlDao hqlDao,
                            EntityManagerSqlDao sqlDao) {
        this.hqlDao = hqlDao;
        this.sqlDao = sqlDao;
    }
}
```

如果应用已经定义同名 Bean，自动配置会退让，不覆盖用户配置。

## 快速开始

### 1. HQL：单条查询

```java
public User findByEmail(String email) throws Exception {
    return hqlDao.find(
        "select u from User u where u.email = ?1",
        User.class,
        email
    );
}
```

位置参数从 **1** 开始，必须与 HQL 中的 `?1`、`?2` 对应。

### 2. HQL：命名参数

```java
Map<String, Object> params = new HashMap<>();
params.put("status", "ACTIVE");
params.put("tenantId", tenantId);

List<User> users = hqlDao.findAll(
    "select u from User u where u.status = :status and u.tenantId = :tenantId",
    User.class,
    params
);
```

### 3. Native SQL：结果映射

```java
List<User> users = sqlDao.findAll(
    "select * from sys_user where status = ?1",
    User.class,
    "ACTIVE"
);
```

`resultClass` 应是 JPA Provider 支持的实体映射类型或结果映射类型。复杂 DTO 投影请使用明确的 `@SqlResultSetMapping`、构造器映射或应用自己的 RowMapper/映射层。

### 4. 分页查询

```java
PageRequest page = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));

List<User> users = hqlDao.findPage(
    "select u from User u where u.status = :status",
    page,
    User.class,
    Collections.singletonMap("status", "ACTIVE")
);
```

`findPage` 只返回当前页内容，不会自动执行 count 查询。`find` 使用 JPA `getSingleResult()` 语义：无结果或结果不唯一时会抛出对应的 JPA 异常。需要总数时，应提供独立且语义匹配的 count HQL / SQL：

```java
Long total = hqlDao.count(
    "select count(u) from User u where u.status = :status",
    Collections.singletonMap("status", "ACTIVE")
);
```

### 5. 更新 / 删除语句

```java
int affected = hqlDao.executeUpdate(
    "update User u set u.status = :status where u.id = :id",
    Map.of("status", "DISABLED", "id", userId)
);
```

Native SQL 可通过 `sqlDao.executeUpdate(...)` 执行。更新和删除语句应放在调用方的 `@Transactional` 方法中执行。

### 6. 动态条件查询

继承 `AbstractBaseService`，实现 `getPredicates`：

```java
@Service
public class UserQuerySupport
        extends AbstractBaseService<UserVo, User, UserQuery> {

    private final UserRepository repository;

    public UserQuerySupport(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public <S extends UserQuery> List<Predicate> getPredicates(
            Root<User> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            S condition) {

        List<Predicate> predicates = new ArrayList<>();

        if (condition != null && condition.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), condition.getStatus()));
        }

        if (condition != null && condition.getKeyword() != null
                && !condition.getKeyword().isBlank()) {
            predicates.add(cb.like(
                root.get("username"),
                "%" + condition.getKeyword().trim() + "%"
            ));
        }

        return predicates;
    }

    // 按业务需要实现 parseVo(...)，并在业务方法中调用 repository。
}
```

动态查询应优先使用 Criteria 参数表达式，不要把用户输入直接拼接到 HQL / SQL 字符串中。

### 7. 批量持久化与事务

```java
@Service
public class UserImportService {

    private final EntityManagerHqlDao hqlDao;

    public UserImportService(EntityManagerHqlDao hqlDao) {
        this.hqlDao = hqlDao;
    }

    @Transactional
    public int importUsers(List<User> users) throws Exception {
        return hqlDao.persistAll(users);
    }
}
```

批量操作由调用方事务管理。实现会按批次 `flush`，不会吞掉持久化异常，也不会擅自 `clear` 整个 Persistence Context，避免意外 detach 调用方正在管理的实体。

> 对超大批量导入，建议根据实体关系、JDBC batch 配置和内存占用压测后选择批次大小；必要时由业务层显式调用 `EntityManager.clear()`。旧的 `insertAll` 入口保留用于兼容，建议迁移到 `persistAll`。Native SQL DAO 的旧 `insertAll` 接收的是 SQL 字符串列表，已视为遗留 API，不建议用于新代码。

## 参数绑定与安全

- 始终使用位置参数或命名参数，不要将用户输入拼接进 SQL / HQL。
- `paramInListToStr` 仅用于兼容旧代码，不应拿来拼接未经校验的用户输入。
- 表名、列名、排序方向通常不能作为 JDBC 参数绑定；如需动态排序，应使用白名单映射。
- Native SQL 的语法、分页和标量返回类型可能随数据库和 Hibernate 版本变化。

## 项目结构

```text
spring-boot-jpa-plus/
├── pom.xml
├── README.md
├── docs/
│   ├── architecture.md
│   ├── dependency-policy.md
│   └── roadmap.md
├── src/
│   ├── main/
│   │   ├── java/io/github/neopen/jpa/
│   │   │   ├── autoconfigure/       # Spring Boot 自动配置
│   │   │   ├── persistence/dao/     # HQL / Native SQL API
│   │   │   ├── convert/             # Specification 与 VO 转换扩展
│   │   │   └── bean/                # 通用分页/响应模型
│   │   └── resources/META-INF/spring/
│   │       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   └── test/
└── examples/
    ├── demo/                        # 从组件主代码中分离的旧业务示例
    └── legacy-web-extension/        # 旧 Web/AOP/幂等/限流辅助代码，不参与核心构建
```

## 构建与测试

```bash
mvn -U clean verify
```

本地安装：

```bash
mvn clean install
```

## 版本兼容策略

- `0.x`：API 可能调整，不承诺二进制兼容。
- `1.x`：核心 API 稳定后再发布；只做向后兼容的功能增强。
- Spring Boot 3.x 使用 Jakarta Persistence。
- Spring Boot 2.x 如需支持，应单独发布兼容 artifact / 分支。

## 参与贡献

欢迎提交 Issue 和 Pull Request。提交前请：

1. 为新增查询 API 补充单元测试或集成测试。
2. 验证至少一种真实数据库（例如 PostgreSQL 或 MySQL）的 Native SQL 行为。
3. 不要把业务实体、Controller、数据库配置或数据库驱动加入核心模块。
4. 说明新增依赖的必要性，并优先使用 Spring Boot BOM 管理版本。

## License

**License:** 尚未确认；正式公开仓库前请由维护者选择许可证并添加对应 `LICENSE` 文件。
