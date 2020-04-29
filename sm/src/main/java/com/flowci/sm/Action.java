package com.flowci.sm;

import java.util.function.Consumer;

public interface Action<T extends Context> extends Consumer<T> {
}
