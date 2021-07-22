package com.flowci.core.flow.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class Template {

    private String url;

    @JsonAlias("url_cn")
    private String urlCn;

    private String desc;

    private boolean isDefault;
}
