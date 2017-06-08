package com.flow.platform.cc.service;

import com.flow.platform.cc.cloud.InstanceManager;
import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.*;
import com.flow.platform.util.zk.ZkException;
import com.flow.platform.util.zk.ZkNodeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
@Service(value = "cmdService")
public class CmdServiceImpl extends ZkServiceBase implements CmdService {

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

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
            // find agent by cmd
            Agent target = selectAgent(cmd);

            // double check agent in zk node
            String agentNodePath = zkHelper.getZkPath(target.getPath());
            if (ZkNodeHelper.exist(zkClient, agentNodePath) == null) {
                throw new AgentErr.NotFoundException(target.getPath().toString());
            }

            // create cmd info
            Cmd cmdInfo = create(cmd);

            // set agent status before cmd sent
            switch (cmd.getType()) {
                case RUN_SHELL:
                    if (target.getStatus() != Agent.Status.IDLE) {
                        // add reject status since busy
                        cmdInfo.addStatus(Cmd.Status.REJECTED);
                        throw new AgentErr.NotAvailableException(cmd.getAgent());
                    }

                    target.setStatus(Agent.Status.BUSY);
                    break;

                case CREATE_SESSION:
                    String sessionId = UUID.randomUUID().toString();
                    cmd.setSessionId(sessionId);
                    target.setSessionId(sessionId);
                    target.setStatus(Agent.Status.BUSY);
                    break;

                case DELETE_SESSION:
                    break;

                case KILL:
                    // DO NOT handle it, agent status from cmd update
                    break;

                case STOP:
                    target.setStatus(Agent.Status.OFFLINE);
                    break;

                case SHUTDOWN:
                    target.setStatus(Agent.Status.OFFLINE);
                    break;
            }

            ZkNodeHelper.setNodeData(zkClient, agentNodePath, cmdInfo.toJson());
            return cmdInfo;

        } catch (AgentErr.NotAvailableException e) {
            // try to start instance
            Zone zone = zoneService.getZone(cmd.getZone());
            InstanceManager instanceManager = zoneService.findInstanceManager(zone);
            if (instanceManager != null) {
                instanceManager.batchStartInstance(AgentService.MIN_IDLE_AGENT_POOL);
            }
            throw e;
        } catch (ZkException.ZkNoNodeException e) {
            throw new AgentErr.NotFoundException(cmd.getAgent());
        } finally {
            mockTrans.unlock();
        }
    }

    @Override
    public void report(String cmdId, Cmd.Status status, CmdResult result) {
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
    public void saveFullLog(String cmdId, MultipartFile file) {
        Cmd cmd = find(cmdId);
        if (cmd == null) {
            throw new IllegalArgumentException("Cmd not exist");
        }

        try {
            Path target = Paths.get(AppConfig.CMD_LOG_DIR.toString(), file.getOriginalFilename());
            Files.write(target, file.getBytes());

            cmd.setFullLogPath(target.toString());
            cmdLoggingQueue.add(target);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Path getFullLog(String cmdId) {
        Cmd cmd = find(cmdId);
        if (cmd == null) {
            throw new IllegalArgumentException("Cmd not exist");
        }

        try {
            Path zippedLogPath = Paths.get(cmd.getFullLogPath());
            if (!Files.exists(zippedLogPath)) {
                throw new IllegalArgumentException("Zipped log file not exist");
            }
            return zippedLogPath;
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Zipped log file not exist");
        }
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
        if (cmd.getSessionId() != null) {
            Agent target = agentService.find(cmd.getSessionId());
            if (target == null) {
                throw new AgentErr.NotFoundException(cmd.getSessionId());
            }
            return target;
        }

        // verify agent path is presented
        AgentPath agentPath = cmd.getAgentPath();
        if (agentPath.getName() == null && cmd.getType() != CmdBase.Type.RUN_SHELL) {
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

            throw new AgentErr.NotAvailableException(cmd.getAgent());
        }

        // find agent by path
        Agent target = agentService.find(agentPath);
        if (target == null) {
            throw new AgentErr.NotFoundException(cmd.getAgent());
        }

        return target;
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
            if (tmp.getType() != Cmd.Type.RUN_SHELL) {
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

        agent.setStatus(isAgentBusy ? Agent.Status.BUSY : Agent.Status.IDLE);
    }
}
