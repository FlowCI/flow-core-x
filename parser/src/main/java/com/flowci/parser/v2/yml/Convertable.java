package com.flowci.parser.v2.yml;

public interface Convertable<T, P> {

    T convert(P ...params);
}
