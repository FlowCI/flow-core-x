package com.flowci.tree;

public interface Nodeable {

    String getName();

    NodePath getPath();

    Nodeable getParent();

    int getOrder();

    void setOrder(int order);

    int getNextOrder();

    void setNextOrder(int order);
}
