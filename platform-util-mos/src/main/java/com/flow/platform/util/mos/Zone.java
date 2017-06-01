package com.flow.platform.util.mos;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by gy@fir.im on 01/06/2017.
 * Copyright fir.im
 */
public class Zone implements Serializable {

    public final static String STATUS_ENABLE = "enable";
    public final static String STATUS_SOLD_OUT = "soldout";

    @SerializedName("availabilityZoneName")
    private String name;

    @SerializedName("availabilityZoneName_CN")
    private String nameCn;

    @SerializedName("availabilityZoneId")
    private String id;

    @SerializedName("Status")
    private String status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameCn() {
        return nameCn;
    }

    public void setNameCn(String nameCn) {
        this.nameCn = nameCn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

        Zone zone = (Zone) o;

        return id.equals(zone.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
