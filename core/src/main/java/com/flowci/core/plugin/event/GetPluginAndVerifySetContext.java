package com.flowci.core.plugin.event;

import com.flowci.domain.Vars;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetPluginAndVerifySetContext extends GetPluginEvent {

    private final Vars<String> context;

    public GetPluginAndVerifySetContext(Object source, String name, Vars<String> context) {
        super(source, name);
        this.context = context;
    }
}
