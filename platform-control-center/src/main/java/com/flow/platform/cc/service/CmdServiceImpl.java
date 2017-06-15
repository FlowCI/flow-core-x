package com.flow.platform.cc.service;

import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.cc.config.TaskConfig;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.util.DateUtil;
import com.flow.platform.domain.*;
import com.flow.platform.util.logger.Logger;
import com.flow.platform.util.zk.ZkException;
import com.flow.platform.util.zk.ZkNodeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
@Service(value = "cmdService")
public class CmdServiceImpl extends ZkServiceBase implements CmdService {

    private final static Logger LOGGER = new Logger(CmdService.class);

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private TaskConfig taskConfig;

    @Autowired
    private Queue<Path> cmdLoggingQueue;

    private final Map<String, Cmd> mockCmdList = new ConcurrentHashMap<>();

    private final ReentrantLock mockTrans = new ReentrantLock();

    @Override
    public Cmd create(CmdBase cmd) {
        String cmdId = UUID.randomUUID().toString();
        Cmd cmdInfo = new Cmd(cmd);
        cmdInfo.setId(cmdId);
        cmdInfo.setSessionId(cmd.getSessionId());
        cmdInfo.setCreatedDate(new Date());
        cmdInfo.setUpdatedDate(new Date());
        mockCmdList.put(cmdId, cmdInfo);
        return cmdInfo;
    }

    @Override
    public Cmd find(String cmdId) {
        return mockCmdList.get(cmdId);
    }

    @Override
    public List<Cmd> listByAgentPath(AgentPath agentPath) {
        List<Cmd> cmdList = new LinkedList<>();
        for (Cmd tmp : mockCmdList.values()) {
            if (!tmp.getAgentPath().equals(agentPath)) {
                continue;
            }
            cmdList.add(tmp);
        }

        cmdList.sort(Comparator.comparing(Cmd::getCreatedDate));
        return cmdList;
    }

    @Override
    public boolean isTimeout(Cmd cmd) {
        if (cmd.getType() != CmdType.RUN_SHELL) {
            throw new UnsupportedOperationException("Check timeout only for run shell");
        }

        // not timeout since cmd is executed
        if (!cmd.isCurrent()) {
            return false;
        }

        ZonedDateTime timeForNow = DateUtil.fromDateForUTC(new Date());

        Date createdAt = cmd.getCreatedDate();
        ZonedDateTime cmdUtcTime = DateUtil.fromDateForUTC(createdAt);

        final long runningInSeconds = ChronoUnit.SECONDS.between(cmdUtcTime, timeForNow);
        return runningInSeconds >= CMD_TIMEOUT_SECONDS;
    }

    /**
     * Send cmd in transaction for agent status
     *
     * @param cmd
     * @return
     */
    @Override
    public Cmd send(CmdBase cmd) {
        mockTrans.lock();

        try {
            Agent target = selectAgent(cmd); // find agent by cmd
            cmd.setAgentPath(target.getPath()); // reset cmd path since some of cmd missing agent name

            // double check agent in zk node
            String agentNodePath = zkHelper.getZkPath(target.getPath());
            if (ZkNodeHelper.exist(zkClient, agentNodePath) == null) {
                throw new AgentErr.NotFoundException(target.getPath().toString());
            }

            Cmd cmdInfo = create(cmd); // create cmd info

            // set agent status before cmd sent
            switch (cmd.getType()) {
                case RUN_SHELL:
                    if (cmdInfo.hasSession()) {
                        break;
                    }

                    // add reject status since busy
                    if (!target.isAvailable()) {
                        cmdInfo.addStatus(CmdStatus.REJECTED);
                        throw new AgentErr.NotAvailableException(target.getName());
                    }

                    target.setStatus(AgentStatus.BUSY);
                    break;

                case CREATE_SESSION:
                    // add reject status since busy
                    if (!target.isAvailable()) {
                        cmdInfo.addStatus(CmdStatus.REJECTED);
                        throw new AgentErr.NotAvailableException(target.getName());
                    }

                    String sessionId = UUID.randomUUID().toString();
                    cmdInfo.setSessionId(sessionId); // set session id to cmd
                    target.setSessionId(sessionId); // set session id to agent
                    target.setSessionDate(new Date());
                    target.setStatus(AgentStatus.BUSY);
                    // target.save
                    break;

                case DELETE_SESSION:
                    // check has current cmd running in agent
                    boolean hasCurrentCmd = false;
                    List<Cmd> agentCmdList = listByAgentPath(target.getPath());
                    for (Cmd cmdItem : agentCmdList) {
                        if (cmdItem.getType() == CmdType.RUN_SHELL && cmdItem.isCurrent()) {
                            hasCurrentCmd = true;
                            break;
                        }
                    }

                    if (!hasCurrentCmd) {
                        target.setStatus(AgentStatus.IDLE);
                    }

                    target.setSessionId(null); // release session from target
                    // target.save
                    break;

                case KILL:
                    // DO NOT handle it, agent status from cmd update
                    break;

                case STOP:
                    target.setStatus(AgentStatus.OFFLINE);
                    break;

                case SHUTDOWN:
                    target.setStatus(AgentStatus.OFFLINE);
                    break;
            }

            ZkNodeHelper.setNodeData(zkClient, agentNodePath, cmdInfo.toJson());
            return cmdInfo;

        } catch (AgentErr.NotAvailableException e) {
            // force to check idle agent
            zoneService.keepIdleAgentTask();
            throw e;
        } catch (ZkException.ZkNoNodeException e) {
            throw new AgentErr.NotFoundException(cmd.getAgent());
        } finally {
            mockTrans.unlock();
        }
    }

    @Override
    public void report(String cmdId, CmdStatus status, CmdResult result) {
        Cmd cmd = find(cmdId);
        if (cmd == null) {
            throw new IllegalArgumentException("Cmd not exist");
        }

        mockTrans.lock();

        try {
            // update cmd status
            cmd.addStatus(status);
            cmd.setResult(result);
            cmd.setUpdatedDate(new Date());

            // update agent status
            updateAgentStatusWhenUpdateCmd(cmd);
        } finally {
            mockTrans.unlock();
        }
    }

    @Override
    public void saveLog(String cmdId, MultipartFile file) {
        Cmd cmd = find(cmdId);
        if (cmd == null) {
            throw new IllegalArgumentException("Cmd not exist");
        }

        try {
            Path target = Paths.get(AppConfig.CMD_LOG_DIR.toString(), file.getOriginalFilename());
            Files.write(target, file.getBytes());

            cmd.getLogPaths().add(target.toString());
            cmdLoggingQueue.add(target);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Scheduled(fixedDelay = 300 * 1000)
    public void checkTimeoutTask() {
        if (!taskConfig.isEnableCmdExecTimeoutTask()) {
            return;
        }
        LOGGER.traceMarker("checkTimeoutTask", "start");

        // find all running status cmd
        for (Cmd cmd : mockCmdList.values()) {
            if (cmd.getType() == CmdType.RUN_SHELL && cmd.isCurrent()) {
                if (isTimeout(cmd)) {
                    // kill current running cmd and report status
                    send(new CmdBase(cmd.getAgentPath(), CmdType.KILL, null));
                    LOGGER.traceMarker("checkTimeoutTask", "Send KILL for timeout cmd %s", cmd);

                    report(cmd.getId(), CmdStatus.TIMEOUT_KILL, cmd.getResult());
                }
            }
        }

        // // TODO: should batch save cmd status
        LOGGER.traceMarker("checkTimeoutTask", "end");
    }

    /**
     * Select agent by AgentPath or session id
     *  - auto select agent if only defined zone name
     *
     * @param cmd
     * @return Agent or null
     * @exception com.flow.platform.cc.exception.AgentErr.NotAvailableException no idle agent in zone
     * @exception com.flow.platform.cc.exception.AgentErr.AgentMustBeSpecified name must for operation cmd type
     * @exception com.flow.platform.cc.exception.AgentErr.NotFoundException target agent not found
     */
    private Agent selectAgent(CmdBase cmd) {
        // check session id as top priority
        if (cmd.hasSession()) {
            Agent target = agentService.find(cmd.getSessionId());
            if (target == null) {
                throw new AgentErr.NotFoundException(cmd.getSessionId());
            }
            return target;
        }

        // verify agent path is presented
        AgentPath agentPath = cmd.getAgentPath();
        if (isAgentPathFail(cmd, agentPath)) {
            throw new AgentErr.AgentMustBeSpecified();
        }

        // auto select agent inside zone
        if (agentPath.getName() == null) {
            List<Agent> availableList = agentService.findAvailable(agentPath.getZone());
            if (availableList.size() > 0) {
                Agent target = availableList.get(0);
                cmd.setAgentPath(target.getPath()); // reset cmd path
                return target;
            }

            throw new AgentErr.NotAvailableException(agentPath.getZone() + ":null");
        }

        // find agent by path
        Agent target = agentService.find(agentPath);
        if (target == null) {
            throw new AgentErr.NotFoundException(cmd.getAgent());
        }

        return target;
    }

    private boolean isAgentPathFail(CmdBase cmd, AgentPath agentPath) {
        if (cmd.getType() == CmdType.CREATE_SESSION || cmd.getType() == CmdType.DELETE_SESSION) {
            return false;
        }
        return agentPath.getName() == null && cmd.getType() != CmdType.RUN_SHELL;
    }

    /**
     * Update agent status when report cmd status and result
     * - busy or idle by Cmd.Type.RUN_SHELL while report cmd status
     *
     * @param cmd Cmd object
     */
    private void updateAgentStatusWhenUpdateCmd(Cmd cmd) {
        // do not update agent status since duration session
        String sessionId = cmd.getSessionId();
        if (sessionId != null && agentService.find(sessionId) != null) {
            return;
        }

        // update agent status by cmd status
        AgentPath agentPath = cmd.getAgentPath();
        boolean isAgentBusy = false;
        for (Cmd tmp : mockCmdList.values()) {
            if (tmp.getType() != CmdType.RUN_SHELL) {
                continue;
            }

            if (!tmp.getAgentPath().equals(agentPath)) {
                continue;
            }

            if (tmp.isCurrent()) {
                isAgentBusy = true;
                break;
            }
        }

        Agent agent = agentService.find(agentPath);
        if (agent == null) {
            throw new IllegalStateException("Cannot find related agent for cmd");
        }

        agent.setStatus(isAgentBusy ? AgentStatus.BUSY : AgentStatus.IDLE);
    }
}
