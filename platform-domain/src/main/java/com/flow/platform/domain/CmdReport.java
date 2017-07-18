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
 * For report cmd status and result
 *
 * @author gy@fir.im
 */
public class CmdReport extends Jsonable {

    // cmd id
    private String id;

    // reported status
    private CmdStatus status;

    // reported result
    private CmdResult result;

    public CmdReport() {
    }

    public CmdReport(String id, CmdStatus status, CmdResult result) {
        this.id = id;
        this.status = status;
        this.result = result;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CmdStatus getStatus() {
        return status;
    }

    public void setStatus(CmdStatus status) {
        this.status = status;
    }

    public CmdResult getResult() {
        return result;
    }

    public void setResult(CmdResult result) {
        this.result = result;
    }
}
