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

/**
 * @author yh@fir.im
 */
public class CmdLog extends Jsonable {

    private String cmdId;
    private String logPath;


    public String getCmdId() {
        return cmdId;
    }

    public void setCmdId(String cmdId) {
        this.cmdId = cmdId;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CmdLog cmdLog = (CmdLog) o;

        return cmdId != null ? cmdId.equals(cmdLog.cmdId) : cmdLog.cmdId == null;
    }

    @Override
    public int hashCode() {
        return cmdId != null ? cmdId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CmdLog{" +
            "cmdId='" + cmdId + '\'' +
            ", logPath='" + logPath + '\'' +
            '}';
    }
}
