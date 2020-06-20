package com.flowci.core.config.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "configuration")
public class TextConfig extends Config {

    private String text;

    public TextConfig() {
        setCategory(Category.TEXT);
    }
}
