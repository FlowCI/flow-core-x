package com.flowci.tree.yml;

import com.flowci.domain.LocalTask;
import com.flowci.domain.StringVars;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class NotifyYml {

    private String plugin;

    private Map<String, String> envs = new LinkedHashMap<>();

    public LocalTask toObj() {
        LocalTask n = new LocalTask();
        n.setPlugin(plugin);
        n.setEnvs(new StringVars(envs));
        return n;
    }
}
