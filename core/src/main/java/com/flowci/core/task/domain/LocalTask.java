package com.flowci.core.task.domain;

import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * The task will be executed in server side
 */
@Getter
@Setter
public abstract class LocalTask {

    @NonNull
    protected String name;

    @NonNull
    protected String jobId;

    protected Vars<String> inputs = new StringVars();

    protected String plugin;

    protected String script;

    protected int timeoutInSecond = 60;

    public boolean hasPlugin() {
        return StringHelper.hasValue(plugin);
    }
}
