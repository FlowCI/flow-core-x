/*
 * Copyright 2018 flow.ci
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

package com.flowci.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.domain.Common.OS;
import com.google.common.base.Strings;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author yang
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(of = {"id"})
public class Agent implements Serializable {

    public final static String PATH_SLASH = "/";

    public enum Status {

        CREATED,

        OFFLINE,

        BUSY,

        IDLE;

        public byte[] getBytes() {
            return this.toString().getBytes();
        }

        public static Status fromBytes(byte[] bytes) {
            return Status.valueOf(new String(bytes));
        }
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Resource {

        private int cpu;

        private int totalMemory; // in MB

        private int freeMemory; // in MB

        private int totalDisk; // in MB

        private int freeDisk; // in MB
    }

    private String id;

    private String name;

    private String token;

    private String url;

    /**
     * Agent host obj id if agent ref on an host
     */
    private String hostId;

    private Common.OS os = OS.UNKNOWN;

    private Resource resource = new Resource();

    private Set<String> tags = Collections.emptySet();

    private Status status = Status.CREATED;

    private Date statusUpdatedAt;

    private String jobId;

    @JsonIgnore
    private SimpleKeyPair rsa;

    public Agent(String name) {
        this.name = name;
    }

    public Agent(String name, Set<String> tags) {
        this.name = name;
        this.tags = tags;
    }

    public void setStatus(Status status) {
        this.status = status;
        this.statusUpdatedAt = new Date();
    }

    @JsonIgnore
    public boolean hasUrl() {
        return !Strings.isNullOrEmpty(url);
    }

    @JsonIgnore
    public boolean hasJob() {
        return !Strings.isNullOrEmpty(jobId);
    }

    @JsonIgnore
    public boolean isBusy() {
        return isOnline() && status == Status.BUSY;
    }

    @JsonIgnore
    public boolean isIdle() {
        return isOnline() && status == Status.IDLE;
    }

    @JsonIgnore
    public boolean isOffline() {
        return status == Status.OFFLINE;
    }

    @JsonIgnore
    public boolean isOnline() {
        return !isOffline();
    }

    @JsonIgnore
    public String getQueueName() {
        return "queue.agent." + id;
    }
}
