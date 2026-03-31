package com.qoobot.opencloud.system.common.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用树形结构构建工具
 * <p>泛型 T 需实现 {@link TreeNode} 接口
 */
public class TreeBuilder {

    private TreeBuilder() {}

    /**
     * 构建树形结构
     *
     * @param nodes  扁平列表（需实现 TreeNode 接口）
     * @param rootId 根节点的 parentId 值（通常为 0L）
     */
    public static <T extends TreeNode<T>> List<T> build(List<T> nodes, Long rootId) {
        if (nodes == null || nodes.isEmpty()) return Collections.emptyList();

        Map<Long, T> map = nodes.stream().collect(Collectors.toMap(TreeNode::getId, n -> n));
        List<T> roots = new ArrayList<>();

        for (T node : nodes) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId.equals(rootId)) {
                roots.add(node);
            } else {
                T parent = map.get(parentId);
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(node);
                }
            }
        }

        sortTree(roots);
        return roots;
    }

    private static <T extends TreeNode<T>> void sortTree(List<T> nodes) {
        if (nodes == null) return;
        nodes.sort(Comparator.comparingInt(TreeNode::getSort));
        nodes.forEach(n -> sortTree(n.getChildren()));
    }
}
