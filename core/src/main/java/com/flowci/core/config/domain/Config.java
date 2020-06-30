package com.flowci.core.config.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.core.common.domain.Mongoable;
import com.flowci.store.Pathable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "configuration")
public abstract class Config extends Mongoable {

    public enum Category {

        SMTP,

        TEXT,

        ANDROID_SIGN
    }

    @Indexed(name = "index_config_name", unique = true)
    private String name;

    private Category category;

    @JsonIgnore
    @Transient
    public Pathable[] getPath() {
        return new Pathable[]{
                () -> "config",
                this::getName,
        };
    }
}
