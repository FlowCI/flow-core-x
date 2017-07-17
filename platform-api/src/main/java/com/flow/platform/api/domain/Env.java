/*
 * *
 *  * Created by yh@fir.im
 *  * Copyright fir.im
 *
 */

package com.flow.platform.api.domain;

import com.flow.platform.domain.Jsonable;

public class Env extends Jsonable {

    private String name;

    private Boolean isProtected;

    public Env(String name, Boolean isProtected) {
        this.name = name;
        this.isProtected = isProtected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getProtected() {
        return isProtected;
    }

    public void setProtected(Boolean aProtected) {
        isProtected = aProtected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Env env = (Env) o;

        return name != null ? name.equals(env.name) : env.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Env{" +
            "name='" + name + '\'' +
            '}';
    }
}
