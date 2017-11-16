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

package com.flow.platform.cc.domain;

import com.flow.platform.domain.Jsonable;

/**
 * Cmd queue data item data structure
 *
 * @author yang
 */
public class CmdQueueItem extends Jsonable {

    private String cmdId;

    private Integer retry;

    public CmdQueueItem() {
    }

    public CmdQueueItem(String cmdId, Integer retry) {
        this.cmdId = cmdId;
        this.retry = retry;
    }

    public String getCmdId() {
        return cmdId;
    }

    public void setCmdId(String cmdId) {
        this.cmdId = cmdId;
    }

    public Integer getRetry() {
        return retry;
    }

    public void setRetry(Integer retry) {
        this.retry = retry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CmdQueueItem that = (CmdQueueItem) o;

        return cmdId.equals(that.cmdId);
    }

    @Override
    public int hashCode() {
        return cmdId.hashCode();
    }

    @Override
    public String toString() {
        return "CmdQueueItem{" +
            "cmdId='" + cmdId + '\'' +
            ", retry=" + retry +
            "} ";
    }
}
