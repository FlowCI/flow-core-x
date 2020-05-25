package com.flowci.core.config.event;

import com.flowci.core.common.event.AbstractSyncEvent;
import com.flowci.core.config.domain.Config;
import lombok.Getter;

@Getter
public class GetConfigEvent extends AbstractSyncEvent<Config> {

    private final String name;

    public GetConfigEvent(Object source, String name) {
        super(source);
        this.name = name;
    }
}
