package com.flowci.core.task;

import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.Setter;

/**
 * The task will be executed in server side
 */
@Getter
@Setter
public class LocalTask {

    private Vars<String> inputs = new StringVars();

    private String plugin;

    private String script;

    public boolean hasPlugin() {
        return StringHelper.hasValue(plugin);
    }
}
