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

package com.flowci.core.agent.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.core.common.domain.Mongoable;
import com.flowci.domain.Common.OS;
import com.flowci.domain.SimpleKeyPair;
import com.flowci.tree.Selector;
import com.google.common.base.Strings;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yang
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Document
public class Agent extends Mongoable {

    public static final String PATH_SLASH = "/";

    public enum Status {

        OFFLINE,

        STARTING, // sent start signal to provider

        IDLE, // started, without running task

        BUSY; // running a task

        public byte[] getBytes() {
            return this.toString().getBytes();
        }

        public static Status fromBytes(byte[] bytes) {
            return Status.valueOf(new String(bytes));
        }
    }

    @Indexed(name = "index_agent_name", unique = true)
    private String name;

    @Indexed(name = "index_agent_token", unique = true)
    private String token;

    private String url;

    /**
     * Agent host obj id if agent ref on an host
     */
    private String hostId;

    private boolean k8sCluster;

    private OS os = OS.UNKNOWN;

    private Set<String> tags = Collections.emptySet();

    private Status status = Status.OFFLINE;

    private Date statusUpdatedAt;

    private String jobId;

    private String containerId; // for started from host

    @JsonIgnore
    private SimpleKeyPair rsa;

    public Agent(String name) {
        this.name = name;
    }

    public Agent(String name, Set<String> tags) {
        this.name = name;
        this.setTags(tags);
    }

    public void setStatus(Status status) {
        this.status = status;
        this.statusUpdatedAt = new Date();
    }

    public void setTags(Set<String> tags) {
        this.tags = tags == null ? Collections.emptySet() : tags;
    }

    @JsonIgnore
    public boolean isStartingOver(int seconds) {
        if (status == Status.STARTING) {
            Instant expire = createdAt.toInstant().plusSeconds(seconds);
            if (Instant.now().isAfter(expire)) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public boolean hasJob() {
        return !Strings.isNullOrEmpty(jobId);
    }

    @JsonIgnore
    public String getQueueName() {
        return "queue.agent." + id;
    }

    @JsonIgnore
    public boolean isBusy() {
        return status == Status.BUSY;
    }

    @JsonIgnore
    public boolean isIdle() {
        return status == Status.IDLE;
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
    public boolean isStarting() {
        return status == Status.STARTING;
    }

    public boolean match(Selector selector) {
        Set<String> labels = new HashSet<>(selector.getLabel());
        if (labels.isEmpty() && tags.isEmpty()) {
            return true;
        }

        labels.retainAll(tags);
        return !labels.isEmpty();
    }
}
