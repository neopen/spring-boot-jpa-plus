/*
 * Copyright 2026 neopen (余一叶知秋尽)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.neopen.core.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TreeUtils} 单元测试。
 */
class TreeUtilsTest {

    /**
     * 验证多级树构建与顺序保留。
     */
    @Test
    void shouldBuildTreeAndKeepOrder() {
        List<Node> nodes = List.of(
                node(2L, 1L, "child-b"),
                node(1L, null, "root"),
                node(3L, 1L, "child-a"),
                node(4L, 2L, "grandchild")
        );

        List<Node> roots = build(nodes);

        assertThat(roots).hasSize(1);

        Node root = roots.get(0);
        assertThat(root.getName()).isEqualTo("root");
        assertThat(root.getChildren())
                .extracting(Node::getName)
                .containsExactly("child-b", "child-a");
        assertThat(root.getChildren().get(0).getChildren())
                .extracting(Node::getName)
                .containsExactly("grandchild");
    }

    /**
     * 验证父节点缺失时按根节点处理。
     */
    @Test
    void shouldTreatMissingParentAsRoot() {
        List<Node> nodes = List.of(
                node(1L, 99L, "orphan"),
                node(2L, 1L, "child")
        );

        List<Node> roots = build(nodes);

        assertThat(roots).extracting(Node::getName)
                .containsExactly("orphan");
        assertThat(roots.get(0).getChildren())
                .extracting(Node::getName)
                .containsExactly("child");
    }

    /**
     * 验证空集合返回空列表。
     */
    @Test
    void shouldReturnEmptyListForEmptyInput() {
        assertThat(build(null)).isEmpty();
        assertThat(build(List.of())).isEmpty();
    }

    /**
     * 验证重复 ID 被拒绝。
     */
    @Test
    void shouldRejectDuplicatedId() {
        List<Node> nodes = List.of(
                node(1L, null, "a"),
                node(1L, null, "b")
        );

        assertThatThrownBy(() -> build(nodes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicated");
    }

    /**
     * 验证空 ID 被拒绝。
     */
    @Test
    void shouldRejectNullId() {
        assertThatThrownBy(() -> build(List.of(node(null, null, "a"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }

    /**
     * 验证自引用被拒绝。
     */
    @Test
    void shouldRejectSelfReference() {
        assertThatThrownBy(() -> build(List.of(node(1L, 1L, "a"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own parent");
    }

    /**
     * 验证环被拒绝。
     */
    @Test
    void shouldRejectCycle() {
        List<Node> nodes = List.of(
                node(1L, 2L, "a"),
                node(2L, 3L, "b"),
                node(3L, 1L, "c")
        );

        assertThatThrownBy(() -> build(nodes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cycle");
    }

    /**
     * 验证已有子集合会被重置。
     */
    @Test
    void shouldResetExistingChildren() {
        Node root = node(1L, null, "root");
        root.getChildren().add(node(9L, null, "stale"));

        build(List.of(root));

        assertThat(root.getChildren()).isEmpty();
    }

    /**
     * 构建树。
     *
     * @param nodes 节点列表
     * @return 根节点
     */
    private static List<Node> build(List<Node> nodes) {
        return TreeUtils.build(
                nodes,
                Node::getId,
                Node::getParentId,
                Node::setChildren
        );
    }

    /**
     * 创建节点。
     *
     * @param id       节点 ID
     * @param parentId 父节点 ID
     * @param name     节点名称
     * @return 节点
     */
    private static Node node(Long id, Long parentId, String name) {
        Node node = new Node();
        node.setId(id);
        node.setParentId(parentId);
        node.setName(name);
        return node;
    }

    /**
     * 测试用树节点。
     */
    static class Node {

        /** 节点 ID。 */
        private Long id;

        /** 父节点 ID。 */
        private Long parentId;

        /** 节点名称。 */
        private String name;

        /** 子节点。 */
        private List<Node> children = new ArrayList<>();

        /**
         * 获取节点 ID。
         *
         * @return 节点 ID
         */
        public Long getId() {
            return id;
        }

        /**
         * 设置节点 ID。
         *
         * @param id 节点 ID
         */
        public void setId(Long id) {
            this.id = id;
        }

        /**
         * 获取父节点 ID。
         *
         * @return 父节点 ID
         */
        public Long getParentId() {
            return parentId;
        }

        /**
         * 设置父节点 ID。
         *
         * @param parentId 父节点 ID
         */
        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }

        /**
         * 获取节点名称。
         *
         * @return 节点名称
         */
        public String getName() {
            return name;
        }

        /**
         * 设置节点名称。
         *
         * @param name 节点名称
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * 获取子节点。
         *
         * @return 子节点列表
         */
        public List<Node> getChildren() {
            return children;
        }

        /**
         * 设置子节点。
         *
         * @param children 子节点列表
         */
        public void setChildren(List<Node> children) {
            this.children = children;
        }
    }
}
