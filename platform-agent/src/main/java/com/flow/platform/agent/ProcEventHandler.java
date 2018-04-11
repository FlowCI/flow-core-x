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

package com.flow.platform.agent;

import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;

import com.flow.platform.util.Logger;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;

/**
 * @author gy@fir.im
 */
@Log4j2
public class ProcEventHandler implements ProcListener {

    private final Cmd cmd;
    private final Map<Cmd, CmdResult> running;
    private final Map<Cmd, CmdResult> finished;
    private final List<ProcListener> extraProcEventListeners;
    private final ReportManager reportManager = ReportManager.getInstance();

    public ProcEventHandler(Cmd cmd,
                            List<ProcListener> extraProcEventListeners,
                            Map<Cmd, CmdResult> running,
                            Map<Cmd, CmdResult> finished) {
        this.cmd = cmd;
        this.extraProcEventListeners = extraProcEventListeners;
        this.running = running;
        this.finished = finished;
    }

    @Override
    public void onStarted(CmdResult result) {
        running.put(cmd, result);

        // report cmd async
        reportManager.cmdReport(cmd.getId(), CmdStatus.RUNNING, result);

        for (ProcListener listener : extraProcEventListeners) {
            listener.onStarted(result);
        }
    }

    @Override
    public void onExecuted(CmdResult result) {
        // report cmd sync since block current thread
        reportManager.cmdReportSync(cmd.getId(), CmdStatus.EXECUTED, result);

        for (ProcListener listener : extraProcEventListeners) {
            listener.onExecuted(result);
        }
    }

    @Override
    public void onLogged(CmdResult result) {
        log.debug("got result...");

        running.remove(cmd);
        finished.put(cmd, result);

        // report cmd sync since block current thread
        reportManager.cmdReportSync(cmd.getId(), CmdStatus.LOGGED, result);

        for (ProcListener listener : extraProcEventListeners) {
            listener.onLogged(result);
        }
    }

    @Override
    public void onException(CmdResult result) {
        running.remove(cmd);
        finished.put(cmd, result);

        // report cmd sync since block current thread
        reportManager.cmdReportSync(cmd.getId(), CmdStatus.EXCEPTION, result);

        for (ProcListener listener : extraProcEventListeners) {
            listener.onException(result);
        }
    }
}
