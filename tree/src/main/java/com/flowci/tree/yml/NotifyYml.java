package com.flowci.tree.yml;

import com.flowci.domain.Notification;
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

    private Boolean enabled;

    public Notification toObj() {
        Notification n = new Notification();
        n.setPlugin(plugin);
        n.setEnabled(enabled == null ? true : enabled);
        n.setInputs(new StringVars(envs));
        return n;
    }
}
