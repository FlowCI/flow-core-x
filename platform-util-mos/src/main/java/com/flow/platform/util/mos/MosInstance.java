package com.flow.platform.util.mos;

import com.flow.platform.domain.Instance;

/**
 * Created by gy@fir.im on 01/06/2017.
 * Copyright fir.im
 */
public final class MosInstance extends Instance {

    public final static String STATUS_DISK = "disk";                        // 0
    public final static String STATUS_START_DEPLOY = "start_deploy";        // 1
    public final static String STATUS_DEPLOYING = "deploying";              // 2
    public final static String STATUS_READY = "ready";                      // 3
    public final static String STATUS_START = "start_start";                // 4
    public final static String STATUS_RUNNING = "running";                  // 5

    private Integer cpu;

    private Integer memory;

    private String secGroupId;

    private String secGroupName;

    private String keypairName;

    private String instanceType;

    private String instanceTypeId;

    private String eips;

    private String billingType;

    public Integer getCpu() {
        return cpu;
    }

    public void setCpu(Integer cpu) {
        this.cpu = cpu;
    }

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public String getSecGroupId() {
        return secGroupId;
    }

    public void setSecGroupId(String secGroupId) {
        this.secGroupId = secGroupId;
    }

    public String getSecGroupName() {
        return secGroupName;
    }

    public void setSecGroupName(String secGroupName) {
        this.secGroupName = secGroupName;
    }

    public String getKeypairName() {
        return keypairName;
    }

    public void setKeypairName(String keypairName) {
        this.keypairName = keypairName;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getInstanceTypeId() {
        return instanceTypeId;
    }

    public void setInstanceTypeId(String instanceTypeId) {
        this.instanceTypeId = instanceTypeId;
    }

    public String getEips() {
        return eips;
    }

    public void setEips(String eips) {
        this.eips = eips;
    }

    public String getBillingType() {
        return billingType;
    }

    public void setBillingType(String billingType) {
        this.billingType = billingType;
    }
}
