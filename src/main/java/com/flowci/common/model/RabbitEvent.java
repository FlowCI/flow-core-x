package com.flowci.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "klass",
        visible = true
)
public interface RabbitEvent extends Serializable {

    @JsonIgnore
    String getRoutingKey();
}
