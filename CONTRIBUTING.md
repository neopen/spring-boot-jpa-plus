# 贡献指南

感谢你对 **Spring Boot JPA Plus** 的关注！本文档说明如何参与贡献，请在提交前仔细阅读。

## 目录

- [行为准则](#行为准则)
- [我可以贡献什么](#我可以贡献什么)
- [开发环境](#开发环境)
- [本地构建与测试](#本地构建与测试)
- [分支与提交规范](#分支与提交规范)
- [Pull Request 流程](#pull-request-流程)
- [代码规范](#代码规范)
- [测试要求](#测试要求)
- [文档要求](#文档要求)
- [Issue 规范](#issue-规范)
- [开发者原创声明（DCO）](#开发者原创声明dco)
- [许可证](#许可证)
- [安全漏洞报告](#安全漏洞报告)

---

## 行为准则

参与本项目即表示你同意遵守 [Contributor Covenant 行为准则](https://www.contributor-covenant.org/version/2/1/code_of_conduct/)。请保持尊重、包容、专业。骚扰、歧视、人身攻击等行为将被处理，严重者会被移出项目。

## 我可以贡献什么

欢迎以下类型的贡献：

- **Bug 修复**：附带复现步骤和测试。
- **新功能**：请先开 Issue 讨论设计，避免大改被拒。
- **文档改进**：README、Javadoc、示例、中文/英文翻译。
- **测试补充**：单元测试、集成测试、边界用例。
- **性能优化**：附 benchmark 数据。
- **依赖升级**：注意协议兼容性（见[许可证](#许可证)）。

以下情况请**先开 Issue 讨论**再动手：

- 新增公共 API 或破坏性变更。
- 引入新依赖（尤其涉及 GPL/LGPL/AGPL 的依赖）。
- 重构核心模块。
- 修改构建/发布流程。

## 开发环境

| 工具 | 版本要求 |
|---|---|
| JDK | 17+ |
| Maven | 3.9+（或 Gradle 8+） |
| Git | 2.30+ |
| IDE | IntelliJ IDEA / VS Code（推荐装 Java 插件） |

克隆并初始化：

```bash
git clone https://github.com/<你的用户名>/spring-boot-jpa-plus.git
cd spring-boot-jpa-plus
./mvnw clean install   # 或 ./gradlew build
```

> 项目使用 Maven Wrapper（`mvnw`）/ Gradle Wrapper（`gradlew`），请优先使用 Wrapper，保证构建环境一致。

## 本地构建与测试

```bash
# 编译
./mvnw clean compile

# 跑全部测试
./mvnw test

# 跑单个测试类
./mvnw test -Dtest=YourTestClass

# 生成第三方协议报告（CI 也会跑）
./mvnw verify

# 检查源文件头
./mvnw license:check
```

提交前请确保：

- `./mvnw verify` 通过。
- 新增/修改代码有对应测试。
- 源文件头检查通过（插件会自动校验）。

## 分支与提交规范

### 分支命名

| 类型 | 格式                  | 示例                          |
| :--- | :-------------------- | :---------------------------- |
| 功能 | `feat/<简短描述>`     | `feat/query-dsl-support`      |
| 修复 | `fix/<简短描述>`      | `fix/n-plus-one-in-batch`     |
| 文档 | `docs/<简短描述>`     | `docs/zh-readme`              |
| 重构 | `refactor/<简短描述>` | `refactor/repository-factory` |
| 杂项 | `chore/<简短描述>`    | `chore/bump-spring-boot`      |

请从 `main` 拉分支，保持分支聚焦单一目的。

### 提交信息（Conventional Commits）

格式：

```
<type>(<scope>): <subject>

<body>

<footer>
```

`type` 取值：

- `feat`：新功能
- `fix`：修复
- `docs`：文档
- `style`：格式（不影响逻辑）
- `refactor`：重构
- `perf`：性能
- `test`：测试
- `build`：构建/依赖
- `ci`：CI 配置
- `chore`：杂项
- `revert`：回滚

示例：

```
feat(query): add lambda-based Specification builder

Add JpaPlusSpecification to support type-safe lambda queries,
reducing boilerplate for common where-clause combinations.

Closes #42
Signed-off-by: Your Name <you@example.com>
```

**每个提交必须带 `Signed-off-by`**（见 DCO）。

## Pull Request 流程

1. **开 Issue（可选但推荐）**：大改动先讨论。
2. **Fork 并拉分支**：从 `main` 拉出聚焦分支。
3. **写代码 + 测试 + 文档**：三者同步更新。
4. **本地验证**：`./mvnw verify` 全绿。
5. **提交并签名**：`git commit -s`。
6. **推送并开 PR**：目标分支为 `main`。
7. **填写 PR 模板**：说明动机、改动、测试方式、关联 Issue。
8. **响应 Review**：按意见修改，保持讨论礼貌。
9. **合并**：维护者会 squash merge 或 rebase merge，保持历史清晰。

PR 检查清单（模板里会体现）：

- [x] 已关联 Issue（如有）

- [x] 有对应测试且全部通过

- [x] 已更新相关文档（README/Javadoc/示例）

- [x] 提交带 `Signed-off-by`

- [x] 未引入不兼容协议的依赖

- [x] 无破坏性变更，或已在 PR 中明确说明并讨论

## 代码规范

- **语言**：Java 17，遵循 [Google Java Style](https://google.github.io/styleguide/javaguide.html)（以项目 Checkstyle 配置为准）。
- **命名**：类 `UpperCamelCase`，方法/变量 `lowerCamelCase`，常量 `UPPER_SNAKE_CASE`。
- **公共 API**：必须写 Javadoc，说明用途、参数、返回值、异常、线程安全性和示例。
- **空值**：公共 API 用 `@Nullable` / `@NonNull` 标注；避免返回 `null` 集合，返回空集合。
- **异常**：不要吞异常；自定义异常继承项目基础异常；错误信息包含可操作提示。
- **日志**：用 SLF4J，禁止 `System.out`；避免在循环里打日志；敏感信息脱敏。
- **依赖注入**：优先构造器注入，避免字段注入。
- **不可变性**：能 `final` 就 `final`；优先不可变对象。
- **文件头**：每个源文件必须带 Apache 2.0 版权头（插件会检查，可用 `./mvnw license:format` 自动补）。

## 测试要求

- 新功能必须有**单元测试**；涉及数据库行为需有**集成测试**（可用 Testcontainers 或 H2）。
- 修复 bug 必须附**复现该 bug 的测试**，确保回归。
- 覆盖率：新增代码行覆盖率建议 ≥ 80%，核心模块不低于现有水平。
- 测试命名：`方法名_场景_期望结果`，如 `findByStatus_whenEmpty_returnsEmptyList`。
- 测试要**独立、可重复**，不依赖执行顺序和外部状态。

## 文档要求

- 公共 API 变更必须同步更新 Javadoc。
- 用户可见的功能变更需更新 README（中英文）和示例代码。
- 新增配置项需在 `spring-configuration-metadata` 或文档中说明。
- 破坏性变更需在 `CHANGELOG.md` 和 PR 描述中显著标注。

## Issue 规范

开 Issue 前请先搜索是否已有同类问题。请使用对应模板：

- **Bug 报告**：环境（JDK/Spring Boot/JPA Plus 版本）、复现步骤、期望行为、实际行为、日志/堆栈、最小复现代码。
- **功能请求**：使用场景、期望 API、替代方案、是否愿意提交 PR。
- **问题咨询**：请用 Discussions，而不是 Issue。

## 开发者原创声明（DCO）

本项目采用 [Developer Certificate of Origin 1.1](https://developercertificate.org/)。**每个提交必须签名**：

```bash
git commit -s -m "feat: your message"
```

这会在提交信息末尾添加：

```
Signed-off-by: Your Name <you@example.com>
```

签名即表示你声明：

1. 该贡献全部或部分由你创作，且你有权按本项目许可证提交；或
2. 该贡献基于此前以兼容许可证发布的作品，且你未修改其许可证；或
3. 该贡献由你以外的人直接提供给你，且你未修改其许可证；或
4. 你理解本项目及其贡献是公开的，贡献记录会永久保留，且你同意按本项目许可证分发。

**未签名的提交会被 CI 拒绝。** 如忘记签名，可用 `git commit --amend -s` 或 `git rebase --signoff` 补签。

> 本项目当前**不使用 CLA**。若未来引入双许可或商业化，会提前公告并说明迁移方式。

## 许可证

提交贡献即表示你同意你的贡献以 **Apache License 2.0** 授权（见 [LICENSE](https://license/)）。

**关于依赖协议**：

- 禁止引入 **GPL / AGPL** 依赖（会传染使用者）。
- 引入 **LGPL** 依赖需在 PR 中说明，并确认仅以调用方式使用、未修改源码。
- 新增依赖需在 [NOTICE](https://notice/) 中登记。
- CI 会运行 `license-maven-plugin` 扫描，发现不兼容协议会失败。

## 安全漏洞报告

**请勿在公开 Issue 中报告安全漏洞。** 请通过以下方式私下联系维护者：

- 邮件：<你的安全联系邮箱>
- 或使用 GitHub 的 [Private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing/privately-reporting-a-security-vulnerability)。

我们会在 48 小时内确认，并在修复后致谢（如你愿意）。

------

再次感谢你的贡献！如有疑问，欢迎在 Discussions 或 Issue 中提问。
