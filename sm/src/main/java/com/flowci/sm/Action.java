package com.flowci.sm;

import java.util.function.Consumer;

public abstract class Action<T extends Context> implements Consumer<T> {

    public boolean canRun(T context) {
        return true;
    }

    public void onException(Throwable e, T context) {
        // ignore by default
    }

    public void onFinally(T context) {
        // ignore by default
    }
}
