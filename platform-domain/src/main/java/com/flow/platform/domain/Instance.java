/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.domain;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Cloud instance base class
 *
 * @author gy@fir.im
 */
public abstract class Instance extends Jsonable {

    /**
     * Cloud provider assigned instance id
     */
    @SerializedName(value = "id", alternate = {"instanceId"})
    protected String id;

    /**
     * Instance name
     */
    @SerializedName(value = "name", alternate = {"instanceName"})
    protected String name;

    /**
     * Instance ip address
     */
    @SerializedName(value = "ip", alternate = {"ipAddresses"})
    protected String ip;

    /**
     * Instance status from provider
     */
    protected String status;

    /**
     * Instance created date
     */
    protected Date createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Instance instance = (Instance) o;

        return id != null ? id.equals(instance.id) : instance.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Instance{" +
                "id='" + id + '\'' +
                "Class='" + getClass().getSimpleName() + '\'' +
                ", name='" + name + '\'' +
                ", ip='" + ip + '\'' +
                "} " + super.toString();
    }
}
