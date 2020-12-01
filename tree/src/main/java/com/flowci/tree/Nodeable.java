package com.flowci.tree;

public interface Nodeable {

    String getName();

    NodePath getPath();

    Nodeable getParent();
}
