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

package com.flow.platform.core.sysinfo;

import com.flow.platform.domain.Jsonable;
import com.google.gson.annotations.Expose;
import java.time.ZonedDateTime;

/**
 * @author yang
 */
public class SystemInfo extends Jsonable {

    public enum Category {

        API,

        CC,

        WEB
    }

    public enum Status {

        RUNNING,

        OFFLINE,

        UNKNOWN
    }

    public enum Type {

        JVM,

        DB,

        SERVER,

        ZK,

        MQ,
    }

    @Expose
    private String name;

    @Expose
    private String version;

    @Expose
    private Status status;

    @Expose
    private ZonedDateTime startTime;

    public SystemInfo() {
    }

    public SystemInfo(Status status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
    }
}
