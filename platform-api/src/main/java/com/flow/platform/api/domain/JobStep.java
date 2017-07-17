/*
 * *
 *  * Created by yh@fir.im
 *  * Copyright fir.im
 *
 */

package com.flow.platform.api.domain;

public class JobStep extends JobNode {

    private Boolean isAllowFailure;

    private String plugin;

    public Boolean getAllowFailure() {
        return isAllowFailure;
    }

    public void setAllowFailure(Boolean allowFailure) {
        isAllowFailure = allowFailure;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }
}
