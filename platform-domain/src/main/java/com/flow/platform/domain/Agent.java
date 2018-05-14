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

import com.google.gson.annotations.Expose;
import java.time.ZonedDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author gy@fir.im
 */
@NoArgsConstructor
@EqualsAndHashCode(of = "path", callSuper = false)
public class Agent extends Webhookable {

    /**
     * Composite key
     */
    @Setter
    @Getter
    @Expose
    private AgentPath path;

    /**
     * Max concurrent proc number
     */
    @Setter
    @Getter
    @Expose
    private Integer concurrentProc = 1;

    /**
     * Agent busy or idle
     */
    @Setter
    @Getter
    @Expose
    private AgentStatus status = AgentStatus.OFFLINE;

    /**
     * Reserved for session id
     */
    @Setter
    @Getter
    @Expose
    private String sessionId;

    /**
     * The date to start session
     */
    @Setter
    @Getter
    @Expose
    private ZonedDateTime sessionDate;

    /**
     * agent token
     */
    @Setter
    @Getter
    @Expose
    private String token;

    /**
     * Created date
     */
    @Setter
    @Getter
    @Expose
    private ZonedDateTime createdDate;

    /**
     * Updated date
     */
    @Setter
    @Getter
    @Expose
    private ZonedDateTime updatedDate;

    public Agent(String zone, String name) {
        this(new AgentPath(zone, name));
    }

    public Agent(AgentPath path) {
        this.path = path;
    }

    public String getZone() {
        return this.path.getZone();
    }

    public String getName() {
        return this.path.getName();
    }

    public boolean isAvailable() {
        return getStatus() == AgentStatus.IDLE;
    }

    @Override
    public String toString() {
        return "Agent{" +
            "zone='" + path.getZone() + '\'' +
            ", name='" + path.getName() + '\'' +
            ", status=" + status + '\'' +
            ", sessionId=" + sessionId +
            '}';
    }
}
