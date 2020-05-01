package com.flowci.zookeeper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.curator.framework.recipes.locks.InterProcessLock;

@Getter
@AllArgsConstructor
public final class InterLock {

    private final String path;

    private final InterProcessLock lock;

}
