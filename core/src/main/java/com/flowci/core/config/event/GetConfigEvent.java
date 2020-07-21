package com.flowci.core.config.event;

import com.flowci.core.common.event.AbstractEvent;
import com.flowci.core.config.domain.Config;
import lombok.Getter;

@Getter
public class GetConfigEvent extends AbstractEvent<Config> {

    private final String name;

    public GetConfigEvent(Object source, String name) {
        super(source);
        this.name = name;
    }
}
