package com.flowci.core.plugin.event;

import com.flowci.core.common.domain.SyncEvent;
import com.flowci.core.plugin.domain.Plugin;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class GetPluginEvent extends ApplicationEvent implements SyncEvent {

    private final String name;

    private Plugin plugin;

    public GetPluginEvent(Object source, String name) {
        super(source);
        this.name = name;
    }
}
