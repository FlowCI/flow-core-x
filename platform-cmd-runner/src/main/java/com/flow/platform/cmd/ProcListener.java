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

package com.flow.platform.cmd;

import com.flow.platform.domain.CmdResult;

/**
 * @author gy@fir.im
 */
public interface ProcListener {

    /**
     * Proc start to exec
     */
    void onStarted(CmdResult result);

    /**
     * Proc log processed
     */
    void onLogged(CmdResult result);

    /**
     * Proc executed without exception (option)
     */
    void onExecuted(CmdResult result);

    /**
     * Proc got exception while executing (option)
     */
    void onException(CmdResult result);
}
