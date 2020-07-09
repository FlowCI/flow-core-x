package com.flowci.core.plugin.event;

import com.flowci.core.common.event.AbstractEvent;
import com.flowci.core.plugin.domain.Plugin;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@Setter
public class GetPluginEvent extends AbstractEvent<Plugin> {

    private final String name;

    private Path dir;

    public GetPluginEvent(Object source, String name) {
        super(source);
        this.name = name;
    }
}
