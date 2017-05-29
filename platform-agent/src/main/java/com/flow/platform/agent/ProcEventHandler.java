package com.flow.platform.agent;

import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;

import java.util.Map;

/**
 * Created by gy@fir.im on 29/05/2017.
 * Copyright fir.im
 */
public class ProcEventHandler implements ProcListener {

    private final Cmd cmd;
    private final Map<Cmd, CmdResult> running;
    private final Map<Cmd, CmdResult> finished;
    private final ReportManager reportManager = ReportManager.getInstance();

    public ProcEventHandler(Cmd cmd, Map<Cmd, CmdResult> running, Map<Cmd, CmdResult> finished) {
        this.cmd = cmd;
        this.running = running;
        this.finished = finished;
    }

    @Override
    public void onStarted(CmdResult result) {
        running.put(cmd, result);

        // report cmd async
        reportManager.cmdReport(cmd.getId(), Cmd.Status.RUNNING, result);
    }

    @Override
    public void onExecuted(CmdResult result) {
        // report cmd sync since block current thread
        reportManager.cmdReportSync(cmd.getId(), Cmd.Status.EXECUTED, result);
    }

    @Override
    public void onLogged(CmdResult result) {
        running.remove(cmd);
        finished.put(cmd, result);

        // report cmd sync since block current thread
        reportManager.cmdReportSync(cmd.getId(), Cmd.Status.LOGGED, result);
    }

    @Override
    public void onException(CmdResult result) {
        running.remove(cmd);
        finished.put(cmd, result);

        // report cmd sync since block current thread
        reportManager.cmdReportSync(cmd.getId(), Cmd.Status.EXCEPTION, result);
    }
}
