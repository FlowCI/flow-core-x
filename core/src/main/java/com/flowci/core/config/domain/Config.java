package com.flowci.core.config.domain;

import com.flowci.core.common.domain.Mongoable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "configuration")
public abstract class Config extends Mongoable {

    public enum Category {

        SMTP,

        FILE,

        JSON
    }

    @Indexed(name = "index_config_name", unique = true)
    private String name;

    private Category category;

}
