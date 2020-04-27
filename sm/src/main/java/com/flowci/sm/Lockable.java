package com.flowci.sm;

public interface Lockable {

    boolean lock(String name);

    boolean unlock(String name);
}
