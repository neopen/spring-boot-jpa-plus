import { defineConfig } from 'vitepress'

const repository = 'https://github.com/neopen/spring-boot-jpa-plus'

export default defineConfig({
    lang: 'zh-CN',
    title: 'Spring Boot JPA Plus',
    description:
        'Spring Boot 的 JPA 增强工具包：动态查询、HQL/SQL 构建器、实体转换、批量操作与分页。',
    base: '/',
    cleanUrls: false,

    themeConfig: {
        nav: [
            { text: '指南', link: '/guide/getting-started' },
            { text: '核心能力', link: '/core/dao' },
            { text: '查询构建器', link: '/query/hql' },
            { text: '转换与扩展', link: '/convert/entity-vo' },
            { text: '参考', link: '/reference/configuration' }
        ],

        sidebar: [
            {
                text: '开始',
                items: [
                    { text: '快速开始', link: '/guide/getting-started' },
                    { text: '模块与依赖', link: '/guide/modules' }
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
                    { text: '逻辑删除', link: '/core/logic-delete' },
                    { text: '审计字段', link: '/core/auditing' },
                    { text: '批量操作', link: '/core/batch' },
                    { text: 'DTO 投影', link: '/core/projection' }
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
