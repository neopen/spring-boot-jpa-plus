# 0.8.0 发布前检查清单

发布方式：在 `dev` 合并后的提交上打 `v0.8.0` 标签，由 `.github/workflows/publish-maven.yml` 完成版本替换、测试、Central 与 GitHub Packages 发布。

## 一、版本与坐标

- [ ] 根 `pom.xml` 与 6 个子模块版本均为 `0.8.0-SNAPSHOT`
- [ ] 标签标签为 `v0.8.0`（CI 会执行 `versions:set`，无需手改发布版本）
- [ ] `CHANGELOG.md` 增加 0.8.0 段落，注明「无新增功能，发布准备版本」
- [ ] `ROADMAP.md`、`docs/version.md`、`docs/en/version.md`、README 版本描述一致
- [ ] 无遗留的 `0.7.0-SNAPSHOT` 引用（CHANGELOG 历史段落除外）

## 二、构建与测试

- [ ] `mvn -B -ntp clean install -Dgpg.skip=true` 全绿
- [ ] SpotBugs 高优先级检查通过（`mvn verify`，根 POM 已配置 `effort=Max`、`threshold=High`）
- [ ] JMH 基线可复现，环境与数据已记录在 `docs/core/benchmark.md` / `docs/en/core/benchmark.md`
- [ ] 测试数量与基线一致（当前 355 个：core 240 / autoconfigure 40 / convert 6 / extension 27 / demo 42）
- [x] `docs` 目录 `npm run build` 通过，无死链
- [ ] 示例工程可本地启动，0.7 新增能力（流式查询、异步审计、JDBC 批量、指标）可演示
- [ ] `mvn -B -ntp verify -Pcentral -Dgpg.skip=true` 能生成 sources / javadoc 制品

## 三、制品与 POM 元数据

- [x] 6 个模块均产出 `-sources.jar` 与 `-javadoc.jar`（已用临时仓库部署验证，插件为根 POM 统一配置）
- [x] 根 POM 含 `name`、`description`、`url`、`licenses`、`developers`、`scm`
- [x] 校验 `scm.developerConnection` 为合法格式：`scm:git:ssh://git@github.com/neopen/spring-boot-jpa-plus.git`（原为占位符，已修正）
- [x] 无 SNAPSHOT 类型的三方依赖（仅本项目版本为 SNAPSHOT）
- [x] `micrometer-core` 保持 `optional`，未引入时自动配置不加载
- [x] `examples/spring-boot-jpa-plus-demo` 不参与发布：`maven.deploy.skip` + `skipPublishing` 已配置

## 四、签名与凭证

- [ ] GitHub Secrets 存在：`GPG_PRIVATE_KEY`、`GPG_PASSPHRASE`
- [ ] GitHub Secrets 存在：`CENTRAL_USERNAME`、`CENTRAL_PASSWORD`
- [ ] 私钥未过期，公钥已上传至公钥服务器
- [ ] 发布签名以 CI 为准（本机 gpg pinentry 不可用，不作为发布环境）
- [ ] `autoPublish=true`、`waitUntil=published` 的发布策略已确认

## 五、文档与迁移

- [x] 0.4 → 0.8 迁移指南已完成并接入站点（中英文）
- [x] 新增页面已注册侧边栏（过滤跳过、指标、JSONB、迁移指南）
- [x] 文档中的配置项与 `JpaPlusProperties` 一一对应（已补齐 `jdbc-batch.*` 与 `metrics.*`）
- [ ] 文档示例代码可编译（至少人工核对）

## 六、兼容性验证

- [x] JDK 17 与 JDK 21 下 `mvn verify` 通过
- [x] Spring Boot 3.2 与 3.5 下自动配置可加载
- [x] Hibernate 6.4 与 6.6 下过滤链、批量、流式查询行为一致
- [x] H2 覆盖主流程
- [x] MySQL 8.0.22 覆盖逻辑删除、租户过滤、批量 upsert、流式查询
- [x] PostgreSQL 验证本期跳过（用户确认；无实例且 Docker 不可用，风险已知）

### 验证记录（2026-11-21）

| 组合 | 实际版本 | 结果 |
| --- | --- | --- |
| JDK 17 × Spring Boot 3.2.12 | Hibernate 6.4.10.Final | 3/3 用例通过 |
| JDK 21 × Spring Boot 3.2.12 | Hibernate 6.4.10.Final | 3/3 用例通过 |
| JDK 17 × Spring Boot 3.5.0 | Hibernate 6.6.15.Final | 3/3 用例通过 |
| JDK 21 × Spring Boot 3.5.0 | Hibernate 6.6.15.Final | 3/3 用例通过 |
| JDK 21 × Spring Boot 3.4.13 × MySQL 8.0.22 | Hibernate 6.6.39.Final | 3/3 用例通过 |
| 库本体 JDK 17 / JDK 21 | 0.8.0-SNAPSHOT | 全量构建通过，355 个测试 |

冒烟用例覆盖：自动配置加载、租户字段填充、逻辑删除与租户过滤链、按业务键批量 upsert、流式查询。
MySQL 用例运行在内置 `test` 库；该账号无 DROP 权限，验证后 `t_widget` 表保留在库中，不影响结论。
临时工程位于 `%TEMP%\jpa-compat\boot32`、`boot35`、`mysql`，不进入仓库。

## 七、发布后验证

- [ ] Central 可解析 `io.github.neopen:spring-boot-jpa-plus-starter:0.8.0`
- [ ] 新建空 Spring Boot 项目引入 starter，启动无报错、自动配置生效
- [ ] GitHub Packages 同步可拉取
- [ ] GitHub Release 已生成并附带发布说明
- [ ] 发布后将 `dev` 版本切换到 `0.9.0-SNAPSHOT`

## 八、回滚预案

- [ ] Central 已发布版本不可删除，缺陷通过 `0.8.1` 补丁版本修复
- [ ] 发布前保留 `v0.8.0` 标签与本地构建产物
- [ ] 若发布中断，先在 Sonatype 后台检查 staging 状态再重试，避免重复部署
