package com.qoobot.opencloud.system.common.util;

import java.util.List;

/**
 * 树节点接口（泛型 T 为节点类型自身）
 */
public interface TreeNode<T> {

    Long getId();

    Long getParentId();

    int getSort();

    List<T> getChildren();

    void setChildren(List<T> children);
}
