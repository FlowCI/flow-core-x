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

package com.flowci.core.githook.domain;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.domain.StringVars;
import com.flowci.exception.NotFoundException;
import com.flowci.util.StringHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author yang
 */
@Getter
@Setter
@ToString(of = {"source", "event"})
@EqualsAndHashCode(of = {"source", "event"})
public abstract class GitTrigger implements Serializable {

    protected final static String SkipMessage = "[ci skip]";

    private GitSource source;

    private GitEvent event;

    public enum GitEvent {

        UNKNOWN,

        PING,

        PUSH,

        PR_OPENED, // pr opened

        PR_MERGED, // pr merged

        TAG
    }

    public StringVars toVariableMap() {
        StringVars map = new StringVars(15);
        map.put(Variables.Git.SOURCE, source.name());
        map.put(Variables.Git.EVENT, event.name());
        map.put(Variables.Git.EVENT_ID, getId());
        return map;
    }

    /**
     * Convert git trigger to job trigger
     */
    public Trigger toJobTrigger() {
        if (event == GitEvent.PUSH) {
            return Trigger.PUSH;
        }

        if (event == GitEvent.TAG) {
            return Trigger.TAG;
        }

        if (event == GitEvent.PR_OPENED) {
            return Trigger.PR_OPENED;
        }

        if (event == GitEvent.PR_MERGED) {
            return Trigger.PR_MERGED;
        }

        throw new NotFoundException("Cannot found related job trigger for {0}", event.name());
    }

    public abstract boolean isSkip();

    /**
     * Get unique git trigger identifier
     */
    public abstract String getId();

    protected static String buildId(String ...props) {
        return StringHelper.toBase64(StringHelper.join(props));
    }
}
