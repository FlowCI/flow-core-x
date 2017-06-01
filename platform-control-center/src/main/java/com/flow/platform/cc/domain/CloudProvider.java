package com.flow.platform.cc.domain;

/**
 * Created by gy@fir.im on 01/06/2017.
 * Copyright fir.im
 */
public enum CloudProvider {

    // meituan cloud
    MOS("MOS", "meituan_mac");

    // provider name
    private String name;

    // for promise lane api
    private String alias;

    CloudProvider(String name, String alias) {
        this.name = name;
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }
}