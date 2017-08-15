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

import com.flow.platform.domain.CmdBase;

/**
 * @author gyfirim
 */
public class CmdQueueItem {
    private String identifier;
    private CmdBase cmdBase;

    public CmdQueueItem(String identifier, CmdBase cmdBase) {
        this.identifier = identifier;
        this.cmdBase = cmdBase;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public CmdBase getCmdBase() {
        return cmdBase;
    }

    public void setCmdBase(CmdBase cmdBase) {
        this.cmdBase = cmdBase;
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

        if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) {
            return false;
        }
        return cmdBase != null ? cmdBase.equals(that.cmdBase) : that.cmdBase == null;
    }

    @Override
    public int hashCode() {
        int result = identifier != null ? identifier.hashCode() : 0;
        result = 31 * result + (cmdBase != null ? cmdBase.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CmdQueueItem{" +
            "identifier='" + identifier + '\'' +
            ", cmdBase=" + cmdBase +
            '}';
    }
}
