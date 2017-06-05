package com.flow.platform.util.mos;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by gy@fir.im on 01/06/2017.
 * Copyright fir.im
 */
public class Instance implements Serializable {

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

    @SerializedName("instanceName")
    private String name;

    private String keypairName;

    private String instanceId;

    private String instanceType;

    private String instanceTypeId;

    private String eips;

    private Date createdAt;

    private String billingType;

    private String ipAddresses;

    private String status;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKeypairName() {
        return keypairName;
    }

    public void setKeypairName(String keypairName) {
        this.keypairName = keypairName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getBillingType() {
        return billingType;
    }

    public void setBillingType(String billingType) {
        this.billingType = billingType;
    }

    public String getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(String ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Instance instance = (Instance) o;

        return instanceId != null ? instanceId.equals(instance.instanceId) : instance.instanceId == null;
    }

    @Override
    public int hashCode() {
        return instanceId != null ? instanceId.hashCode() : 0;
    }
}
