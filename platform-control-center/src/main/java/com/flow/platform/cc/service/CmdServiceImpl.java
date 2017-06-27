package com.flow.platform.cc.service;

import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.cc.config.TaskConfig;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.dao.*;
import com.flow.platform.cc.task.CmdWebhookTask;
import com.flow.platform.util.DateUtil;
import com.flow.platform.domain.*;
import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.ZkException;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
@Service(value = "cmdService")
@Transactional(isolation = Isolation.REPEATABLE_READ)
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

    @Autowired
    private CmdDao cmdDao;

    @Autowired
    private CmdResultDao cmdResultDao;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private Executor taskExecutor;

    private final ReentrantLock mockTrans = new ReentrantLock();

    @Override
    public Cmd create(CmdInfo info) {
        String cmdId = UUID.randomUUID().toString();
        Cmd cmd = Cmd.convert(info);
        cmd.setId(cmdId);
        cmd.setCreatedDate(new Date());
        cmd.setUpdatedDate(new Date());
        cmdDao.save(cmd);
        return cmd;
    }

    @Override
    public Cmd find(String cmdId) {
        return cmdDao.find(cmdId);
    }

    @Override
    public List<Cmd> listByAgentPath(AgentPath agentPath) {
        return cmdDao.listByAgentPath(agentPath);
    }

    @Override
    public List<Cmd> listByZone(String zone) {
        return cmdDao.listByZone(zone);
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
     * @param cmdInfo
     * @return
     */
    @Override
    public Cmd send(CmdInfo cmdInfo) {
        mockTrans.lock();

        try {
            Agent target = selectAgent(cmdInfo); // find agent by cmd
            cmdInfo.setAgentPath(target.getPath()); // reset cmd path since some of cmd missing agent name

            // double check agent in zk node
            String agentNodePath = zkHelper.getZkPath(target.getPath());

            if (ZkNodeHelper.exist(zkClient, agentNodePath) == null) {
                throw new AgentErr.NotFoundException(target.getPath().toString());
            }

            Cmd cmd = create(cmdInfo); // create cmd info

            // set agent status before cmd sent
            switch (cmdInfo.getType()) {
                case RUN_SHELL:
                    if (cmd.hasSession()) {
                        break;
                    }

                    // add reject status since busy
                    if (!target.isAvailable()) {
                        updateStatus(cmd.getId(), CmdStatus.REJECTED, null, true);
                        throw new AgentErr.NotAvailableException(target.getName());
                    }

                    target.setStatus(AgentStatus.BUSY);
                    break;

                case CREATE_SESSION:
                    // add reject status since unable to create session for agent
                    String sessionId = agentService.createSession(target);
                    if (sessionId == null) {
                        updateStatus(cmd.getId(), CmdStatus.REJECTED, null, false);
                        throw new AgentErr.NotAvailableException(target.getName());
                    }

                    // set session id to cmd and save
                    cmd.setSessionId(sessionId);
                    cmdDao.update(cmd);

                    break;

                case DELETE_SESSION:
                    agentService.deleteSession(target);
                    break;

                case KILL:
                    // DO NOT handle it, agent status from cmd update
                    break;

                case STOP:
                    agentService.deleteSession(target);
                    target.setStatus(AgentStatus.OFFLINE);
                    break;

                case SHUTDOWN:
                    // in shutdown action, cmd content is sudo password
                    if (Strings.isNullOrEmpty(cmd.getCmd())) {
                        throw new IllegalArgumentException("For SHUTDOWN action, password of 'sudo' must be provided");
                    }

                    agentService.deleteSession(target);
                    target.setStatus(AgentStatus.OFFLINE);
                    break;
            }

            agentDao.update(target);
            ZkNodeHelper.setNodeData(zkClient, agentNodePath, cmd.toJson());

            return cmd;

        } catch (AgentErr.NotAvailableException e) {
            // force to check idle agent
            zoneService.keepIdleAgentTask();
            throw e;
        } catch (ZkException.ZkNoNodeException e) {
            throw new AgentErr.NotFoundException(cmdInfo.getAgentName());
        } finally {
            mockTrans.unlock();
        }
    }

    @Override
    public void updateStatus(String cmdId, CmdStatus status, CmdResult result, boolean updateAgentStatus) {
        Cmd cmd = find(cmdId);
        if (cmd == null) {
            throw new IllegalArgumentException("Cmd not exist");
        }

        mockTrans.lock();

        try {
            // compare exiting cmd result and update
            if (result != null) {
                CmdResult cmdResult = cmdResultDao.findByCmdId(cmd.getId());
                if (cmdResult != null) {
                    coverCmdResult(cmdResult, result);
                    cmdResultDao.update(cmdResult);
                } else {
                    cmdResult = result;
                    cmdResult.setCmdId(cmdId);
                    cmdResultDao.save(cmdResult);
                }
            }

            // update cmd status
            if (cmd.addStatus(status)) {
                cmd.setUpdatedDate(new Date());
                cmdDao.update(cmd);
                // update agent status
                if (updateAgentStatus) {
                    updateAgentStatusWhenUpdateCmd(cmd);
                }
            }
        } finally {
            mockTrans.unlock();

            // try to call webhhook of cmd
            webhookCallback(cmd);
        }
    }

    private CmdResult coverCmdResult(CmdResult source, CmdResult dest) {
        if (dest.getFinishTime() != null) {
            source.setStartTime(dest.getStartTime());
            source.setFinishTime(dest.getFinishTime());
            source.setExecutedTime(dest.getExecutedTime());
            source.setDuration(dest.getDuration());
            source.setExitValue(dest.getExitValue());
            source.setProcess(dest.getProcess());
            source.setProcessId(dest.getProcessId());
            source.setTotalDuration(dest.getTotalDuration());
        }
        return source;
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
            cmdDao.update(cmd);
            cmdLoggingQueue.add(target);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void webhookCallback(CmdBase cmdBase) {
        if (cmdBase == null || cmdBase.getWebhook() == null) {
            return;
        }

        taskExecutor.execute(new CmdWebhookTask(cmdBase));
    }

    @Scheduled(fixedDelay = 300 * 1000)
    public void checkTimeoutTask() {
        if (!taskConfig.isEnableCmdExecTimeoutTask()) {
            return;
        }
        LOGGER.traceMarker("checkTimeoutTask", "start");

        // find all running status cmd
        for (Cmd cmd : getRunningCmds()) {
            if (cmd.getType() == CmdType.RUN_SHELL && cmd.isCurrent()) {
                if (isTimeout(cmd)) {
                    // kill current running cmd and report status
                    send(new CmdInfo(cmd.getAgentPath(), CmdType.KILL, null));
                    LOGGER.traceMarker("checkTimeoutTask", "Send KILL for timeout cmd %s", cmd);
                    updateStatus(cmd.getId(), CmdStatus.TIMEOUT_KILL, null, true);
                }
            }
        }

        // // TODO: should batch save cmd status
        LOGGER.traceMarker("checkTimeoutTask", "end");
    }

    /**
     * Select agent by AgentPath or session id
     * - auto select agent if only defined zone name
     *
     * @param cmd
     * @return Agent or null
     * @throws com.flow.platform.cc.exception.AgentErr.NotAvailableException no idle agent in zone
     * @throws com.flow.platform.cc.exception.AgentErr.AgentMustBeSpecified  name must for operation cmd type
     * @throws com.flow.platform.cc.exception.AgentErr.NotFoundException     target agent not found
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
            throw new AgentErr.NotFoundException(cmd.getAgentName());
        }

        return target;
    }

    private List<Cmd> getRunningCmds() {
        return cmdDao.listByStatus(CmdStatus.RUNNING.toString());
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
        for (Cmd tmp : listByAgentPath(agentPath)) {
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
        agentDao.update(agent);
    }
}
