package com.flowci.common.sm;

public abstract class Action<T extends Context> {

    public boolean canRun(T context) {
        return true;
    }

    public abstract void accept(T t) throws Exception;

    public void onException(Throwable e, T context) {
        // ignore by default
    }

    public void onFinally(T context) {
        // ignore by default
    }
}
