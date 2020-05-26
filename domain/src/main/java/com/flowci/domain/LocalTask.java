package com.flowci.domain;

import com.flowci.util.StringHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * The task will be executed in server side
 */
@Getter
@Setter
@EqualsAndHashCode(of = "plugin")
public class LocalTask {

    private String plugin;

    private Vars<String> envs = new StringVars();

    public boolean hasPlugin() {
        return StringHelper.hasValue(plugin);
    }
}
