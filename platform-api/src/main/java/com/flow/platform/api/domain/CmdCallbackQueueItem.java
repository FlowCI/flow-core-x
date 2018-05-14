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

package com.flow.platform.api.domain;

import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.Jsonable;
import java.math.BigInteger;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yh@firim
 */

@ToString(of = {"jobId", "cmd"})
public class CmdCallbackQueueItem extends Jsonable {

    @Getter
    private final BigInteger jobId;

    @Getter
    private final String path; // node path

    @Getter
    private final Cmd cmd;

    // default retry times 5
    @Getter
    @Setter
    private Integer retryTimes = 5;

    public CmdCallbackQueueItem(BigInteger jobId, Cmd cmd) {
        this.jobId = jobId;
        this.cmd = cmd;
        this.path = cmd.getExtra();
    }

    /**
     * self plus ++
     */
    public void plus() {
        retryTimes += 1;
    }
}
