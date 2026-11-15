import DefaultTheme from 'vitepress/theme'
import { h } from 'vue'
import './custom.css'

export default {
    extends: DefaultTheme,
    Layout() {
        return h(DefaultTheme.Layout, null, {
            'doc-after': () =>
                h('div', { class: 'feedback' }, [
                    '文档有误或有疑问？请到 ',
                    h(
                        'a',
                        {
                            href: 'https://github.com/neopen/spring-boot-jpa-plus/issues',
                            target: '_blank',
                            rel: 'noreferrer'
                        },
                        'GitHub Issues'
                    ),
                    ' 反馈。'
                ])
        })
    }
}
