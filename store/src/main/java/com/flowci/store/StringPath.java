package com.flowci.store;

import lombok.Getter;

public class StringPath implements Pathable {

    @Getter
    private final String name;

    public StringPath(String name) {
        this.name = name;
    }

    @Override
    public String pathName() {
        return this.name;
    }
}
