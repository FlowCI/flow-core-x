package com.flowci.core.common.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.flowci.common.helper.StringHelper;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public abstract class SourceWithDomain implements Serializable {

    private static final String DOMAIN_CN = "cn";

    @JsonAlias("url")
    private String source;

    @JsonAlias("url_cn")
    @JsonProperty("source_cn")
    private String sourceCn;

    public String getSourceWithDomain(String domain) {
        if (StringHelper.isEmpty(domain)) return source;
        return DOMAIN_CN.equalsIgnoreCase(domain) ? sourceCn : source;
    }
}
