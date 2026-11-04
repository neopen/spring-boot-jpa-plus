# Spring Boot JPA Plus

<p align="center">  <strong>Make JPA Development Simpler.</strong>  <br/>  一个轻量、模块化的 Spring Boot JPA 增强工具库。</p>

<p align="center">  <a href="https://github.com/neopen/spring-boot-jpa-plus">    <img src="https://img.shields.io/github/stars/neopen/spring-boot-jpa-plus?style=flat-square" alt="Stars"/>  </a>  <img src="https://img.shields.io/github/license/neopen/spring-boot-jpa-plus?style=flat-square" alt="License"/>  <img src="https://img.shields.io/github/last-commit/neopen/spring-boot-jpa-plus?style=flat-square" alt="Last Commit"/></p>

## ✨ Introduction

**Spring Boot JPA Plus** 是一个基于 Spring Data JPA 构建的增强工具库。

它针对日常开发中重复的 CRUD、批量操作、动态查询、Entity-VO 转换及分页处理等场景进行封装，让 JPA 开发更加简洁、高效。

不改变原有 JPA 开发习惯，通过模块化设计和可选扩展，让常用功能开箱即用。

## 🚀 Features

- **CRUD** — 简化常用数据增删改查操作。
- **Batch Operations** — 提供便捷的批量数据操作。
- **Dynamic Query** — 基于 Criteria API 灵活构建动态查询。
- **Entity-VO Converter** — 简化实体对象与业务视图对象转换。
- **Pagination** — 统一分页查询与结果封装。
- **HQL / SQL** — 扩展常用数据访问能力。
- **Auto Configuration** — Spring Boot 自动配置，减少重复配置。
- **Modular Extensions** — 模块化设计，功能按需引入。

## 📦 Modules

| Module          | Description              |
| --------------- | ------------------------ |
| `core`          | 核心 JPA 数据访问能力    |
| `convert`       | Entity-VO 转换与动态查询 |
| `autoconfigure` | Spring Boot 自动配置     |
| `starter`       | 统一依赖入口             |
| `extension`     | 可选功能扩展             |
| `demo`          | 示例项目                 |

## ⚡ Quick Start

添加 Starter 依赖：

```
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-starter</artifactId>
    <version>${version}</version>
</dependency>
```

将 `${version}` 替换为实际发布版本。

引入依赖后，即可在业务服务中使用项目提供的基础服务、查询构建及转换能力。

## 💡 Example

通过 `BasePredicateService` 扩展动态查询条件：

```
@Override
public List<Predicate> buildPredicates(
        Root<Order> root,
        CriteriaQuery<?> query,
        CriteriaBuilder cb,
        OrderQueryParam param) {

    List<Predicate> predicates = new ArrayList<>();

    if (param == null) {
        return predicates;
    }

    if (StrUtil.isNotBlank(param.getOrderNo())) {
        predicates.add(cb.like(
                root.get("orderNo"),
                "%" + param.getOrderNo() + "%"
        ));
    }

    if (StrUtil.isNotBlank(param.getStatus())) {
        predicates.add(cb.equal(
                root.get("status"),
                param.getStatus()
        ));
    }

    return predicates;
}
```

将业务查询条件与通用查询逻辑分离，让代码更清晰、更易维护。

## 🛠️ Tech Stack

```
Java` · `Spring Boot` · `Spring Data JPA` · `Hibernate` · `Maven
```

## 📖 Demo

查看示例项目：

[ spring-boot-jpa-plus-demo ](https://github.com/neopen/spring-boot-jpa-plus/tree/main/examples/spring-boot-jpa-plus-demo)

## 🤝 Contributing

欢迎提交 Issue、功能建议和 Pull Request，一起完善项目。

## 📄 License

[MIT License](https://chatgpt.com/c/LICENSE)

<p align="center">  If it helps you, give it a ⭐</p>