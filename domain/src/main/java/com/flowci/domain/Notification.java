package com.flowci.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode(of = "plugin")
public final class Notification {

    /**
     * Plugin name which has tag 'notification'
     */
    private String plugin;

    private Vars<String> inputs = new StringVars();

    private boolean enabled;
}
