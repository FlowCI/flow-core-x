package com.flowci.core.flow.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class Template {

    private String url;

    private String desc;

    private boolean isDefault;
}
