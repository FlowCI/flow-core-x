package com.flowci.core.config.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextConfig extends Config {

    private String text;

    public TextConfig() {
        setCategory(Category.TEXT);
    }
}
