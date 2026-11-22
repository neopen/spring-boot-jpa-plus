import { defineConfig } from 'vitepress'

const repository = 'https://github.com/neopen/spring-boot-jpa-plus'

export default defineConfig({
    lang: 'zh-CN',
    title: 'Spring Boot JPA Plus',
    description:
        'Spring Boot 的 JPA 增强工具包：动态查询、HQL/SQL 构建器、实体转换、批量操作与分页。',
    base: '/',
    cleanUrls: false,

    locales: {
        root: {
            label: '简体中文',
            lang: 'zh-CN'
        },
        en: {
            label: 'English',
            lang: 'en-US',
            link: '/en/',
            title: 'Spring Boot JPA Plus',
            description:
                'A JPA enhancement toolkit for Spring Boot: dynamic queries, HQL/SQL builders, entity conversion, batch operations, and pagination.',
            themeConfig: {
                nav: [
                    { text: 'Guide', link: '/en/guide/getting-started' },
                    { text: 'Core', link: '/en/core/dao' },
                    { text: 'Query Builders', link: '/en/query/hql' },
                    {
                        text: 'Convert & Extend',
                        link: '/en/convert/entity-vo'
                    },
                    {
                        text: 'Reference',
                        link: '/en/reference/configuration'
                    },
                    { text: 'Releases', link: '/en/version' }
                ],

                sidebar: [
                    {
                        text: 'Getting Started',
                        items: [
                            {
                                text: 'Quick Start',
                                link: '/en/guide/getting-started'
                            },
                            {
                                text: 'Modules & Dependencies',
                                link: '/en/guide/modules'
                            },
                            {
                                text: 'Migrating 0.4 → 0.8',
                                link: '/en/guide/migration-0.4-to-0.8'
                            },
                            {
                                text: 'Releases',
                                link: '/en/version'
                            }
                        ]
                    },
                    {
                        text: 'Core',
                        items: [
                            { text: 'DAO & CRUD', link: '/en/core/dao' },
                            {
                                text: 'Dynamic Queries',
                                link: '/en/core/query-wrapper'
                            },
                            {
                                text: 'Specification Builder',
                                link: '/en/core/specification'
                            },
                            {
                                text: 'Specification Executor',
                                link: '/en/core/executor'
                            },
                            {
                                text: 'Annotation Conditions',
                                link: '/en/core/conditions'
                            },
                            { text: 'Safe Sorting', link: '/en/core/sorting' },
                            {
                                text: 'Keyset Pagination',
                                link: '/en/core/keyset-pagination'
                            },
                            {
                                text: 'Logical Deletion',
                                link: '/en/core/logic-delete'
                            },
                            { text: 'Auditing', link: '/en/core/auditing' },
                            {
                                text: 'Change History',
                                link: '/en/core/audit-trail'
                            },
                            {
                                text: 'Multi-Tenancy',
                                link: '/en/core/tenant'
                            },
                            {
                                text: 'Data Permission',
                                link: '/en/core/data-permission'
                            },
                            {
                                text: 'Skipping Filters',
                                link: '/en/core/filter-ignore'
                            },
                            {
                                text: 'Context Propagation',
                                link: '/en/core/context-propagation'
                            },
                            { text: 'Batch Operations', link: '/en/core/batch' },
                            {
                                text: 'Dynamic Data Sources',
                                link: '/en/core/datasource'
                            },
                            { text: 'Metrics', link: '/en/core/metrics' },
                            {
                                text: 'Performance Baseline',
                                link: '/en/core/benchmark'
                            },
                            { text: 'PostgreSQL JSONB', link: '/en/core/jsonb' },
                            { text: 'DTO Projection', link: '/en/core/projection' },
                            {
                                text: 'Enum Conversion',
                                link: '/en/core/enum-convert'
                            },
                            {
                                text: 'Field Encryption',
                                link: '/en/core/encryption'
                            },
                            { text: 'Tree Structure', link: '/en/core/tree' },
                            { text: 'Snowflake ID', link: '/en/core/id-generator' }
                        ]
                    },
                    {
                        text: 'Query Builders',
                        items: [
                            { text: 'HQL Queries', link: '/en/query/hql' },
                            { text: 'Native SQL', link: '/en/query/sql' }
                        ]
                    },
                    {
                        text: 'Convert & Extend',
                        items: [
                            {
                                text: 'Entity → VO',
                                link: '/en/convert/entity-vo'
                            },
                            {
                                text: 'Param → Entity',
                                link: '/en/convert/param-mapper'
                            },
                            {
                                text: 'Idempotency',
                                link: '/en/extension/idempotent'
                            },
                            {
                                text: 'Rate Limiting',
                                link: '/en/extension/rate-limit'
                            },
                            {
                                text: 'Operation Logging',
                                link: '/en/extension/logging'
                            },
                            {
                                text: 'Data Desensitization',
                                link: '/en/extension/desensitize'
                            },
                            {
                                text: 'Optimistic-lock Retry',
                                link: '/en/extension/retry'
                            },
                            {
                                text: 'Slow Method Logging',
                                link: '/en/extension/slow-log'
                            },
                            {
                                text: 'Paged Responses',
                                link: '/en/extension/response'
                            }
                        ]
                    },
                    {
                        text: 'Reference',
                        items: [
                            {
                                text: 'Configuration',
                                link: '/en/reference/configuration'
                            },
                            { text: 'API Index', link: '/en/reference/api' },
                            { text: 'Examples', link: '/en/examples' }
                        ]
                    }
                ],

                search: {
                    provider: 'local'
                },

                socialLinks: [{ icon: 'github', link: repository }],

                lastUpdated: {
                    text: 'Last updated',
                    formatOptions: {
                        dateStyle: 'short',
                        timeStyle: 'short'
                    }
                },

                outline: {
                    label: 'On this page',
                    level: [2, 3]
                },

                docFooter: {
                    prev: 'Previous',
                    next: 'Next'
                },

                footer: {
                    message: 'Released under the MIT License',
                    copyright: 'Copyright © 2026 NeoPen'
                },

                sidebarMenuLabel: 'Menu',
                returnToTopLabel: 'Return to top',
                darkModeSwitchLabel: 'Appearance',
                lightModeSwitchTitle: 'Switch to light theme',
                darkModeSwitchTitle: 'Switch to dark theme'
            }
        }
    },

    themeConfig: {
        nav: [
            { text: '指南', link: '/guide/getting-started' },
            { text: '核心能力', link: '/core/dao' },
            { text: '查询构建器', link: '/query/hql' },
            { text: '转换与扩展', link: '/convert/entity-vo' },
            { text: '参考', link: '/reference/configuration' },
            { text: '版本发布', link: '/version' }
        ],

        sidebar: [
            {
                text: '开始',
                items: [
                    { text: '快速开始', link: '/guide/getting-started' },
                    { text: '模块与依赖', link: '/guide/modules' },
                    { text: '从 0.4 迁移到 0.8', link: '/guide/migration-0.4-to-0.8' },
                    { text: '版本发布', link: '/version' }
                ]
            },
            {
                text: '核心能力',
                items: [
                    { text: 'DAO 与 CRUD', link: '/core/dao' },
                    { text: '动态条件查询', link: '/core/query-wrapper' },
                    { text: 'Specification 构建器', link: '/core/specification' },
                    { text: 'Specification 执行器', link: '/core/executor' },
                    { text: '注解式查询条件', link: '/core/conditions' },
                    { text: '安全排序', link: '/core/sorting' },
                    { text: '游标分页', link: '/core/keyset-pagination' },
                    { text: '逻辑删除', link: '/core/logic-delete' },
                    { text: '审计字段', link: '/core/auditing' },
                    { text: '变更历史', link: '/core/audit-trail' },
                    { text: '多租户', link: '/core/tenant' },
                    { text: '行级数据权限', link: '/core/data-permission' },
                    { text: '过滤跳过', link: '/core/filter-ignore' },
                    { text: '跨线程上下文传播', link: '/core/context-propagation' },
                    { text: '批量操作', link: '/core/batch' },
                    { text: '动态数据源', link: '/core/datasource' },
                    { text: '指标监控', link: '/core/metrics' },
                    { text: '性能基线', link: '/core/benchmark' },
                    { text: 'PostgreSQL JSONB', link: '/core/jsonb' },
                    { text: 'DTO 投影', link: '/core/projection' },
                    { text: '枚举值转换', link: '/core/enum-convert' },
                    { text: '字段加密', link: '/core/encryption' },
                    { text: '树形结构', link: '/core/tree' },
                    { text: '雪花 ID', link: '/core/id-generator' }
                ]
            },
            {
                text: '查询构建器',
                items: [
                    { text: 'HQL 查询', link: '/query/hql' },
                    { text: '原生 SQL 查询', link: '/query/sql' }
                ]
            },
            {
                text: '转换与扩展',
                items: [
                    { text: 'Entity → VO 转换', link: '/convert/entity-vo' },
                    { text: '参数 → 实体映射', link: '/convert/param-mapper' },
                    { text: '幂等控制', link: '/extension/idempotent' },
                    { text: '接口限流', link: '/extension/rate-limit' },
                    { text: '操作日志注解', link: '/extension/logging' },
                    { text: '数据脱敏', link: '/extension/desensitize' },
                    { text: '乐观锁重试', link: '/extension/retry' },
                    { text: '慢方法日志', link: '/extension/slow-log' },
                    { text: '统一分页响应', link: '/extension/response' }
                ]
            },
            {
                text: '参考',
                items: [
                    { text: '配置项', link: '/reference/configuration' },
                    { text: 'API 索引', link: '/reference/api' },
                    { text: '示例项目', link: '/examples' }
                ]
            }
        ],

        search: {
            provider: 'local'
        },

        socialLinks: [{ icon: 'github', link: repository }],

        lastUpdated: {
            text: '最后更新',
            formatOptions: { dateStyle: 'short', timeStyle: 'short' }
        },

        outline: {
            label: '本页目录',
            level: [2, 3]
        },

        docFooter: {
            prev: '上一篇',
            next: '下一篇'
        },

        footer: {
            message: '基于 MIT 许可证发布',
            copyright: 'Copyright © 2026 NeoPen'
        },

        sidebarMenuLabel: '菜单',
        returnToTopLabel: '回到顶部',
        darkModeSwitchLabel: '主题',
        lightModeSwitchTitle: '切换到浅色模式',
        darkModeSwitchTitle: '切换到深色模式'
    }
})
