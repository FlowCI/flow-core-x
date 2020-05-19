package com.flowci.core.flow.domain;

import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public final class Notification {

    /**
     * Plugin name which has tag 'notification'
     */
    private String plugin;

    /**
     * Fetch image from plugin first, otherwise apply this default image to local task
     */
    private String image = "flowci/plugin-runtime:1.0";

    private Vars<String> inputs = new StringVars();
}
