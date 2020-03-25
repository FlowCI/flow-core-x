/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.flow.service;

import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitChannelOperation;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.service.SecretService;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.FlowUserDao;
import com.flowci.core.flow.dao.YmlDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Flow.Status;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.flow.event.FlowConfirmedEvent;
import com.flowci.core.flow.event.FlowCreatedEvent;
import com.flowci.core.flow.event.FlowDeletedEvent;
import com.flowci.core.flow.event.FlowInitEvent;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.event.CreateNewJobEvent;
import com.flowci.core.trigger.domain.GitPingTrigger;
import com.flowci.core.trigger.domain.GitPushTrigger;
import com.flowci.core.trigger.domain.GitTrigger;
import com.flowci.core.trigger.domain.GitTrigger.GitEvent;
import com.flowci.core.trigger.event.GitHookEvent;
import com.flowci.core.user.event.UserDeletedEvent;
import com.flowci.domain.*;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.store.FileManager;
import com.flowci.tree.*;
import com.flowci.util.StringHelper;
import com.google.common.collect.Sets;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.util.*;

/**
 * @author yang
 */
@Log4j2
@Service
public class FlowServiceImpl implements FlowService {

    @Autowired
    private String serverUrl;

    @Autowired
    private ConfigProperties.RabbitMQ rabbitProperties;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private YmlDao ymlDao;

    @Autowired
    private FlowUserDao flowUserDao;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private SecretService secretService;

    @Autowired
    private RabbitChannelOperation jobQueueManager;

    // ====================================================================
    // %% Public function
    // ====================================================================

    @Override
    public List<Flow> list(Status status) {
        String userId = sessionManager.getUserId();
        return list(userId, status);
    }

    @Override
    public List<Flow> listByCredential(String credentialName) {
        Secret secret = secretService.get(credentialName);

        List<Flow> list = list(Status.CONFIRMED);
        Iterator<Flow> iter = list.iterator();

        for (; iter.hasNext(); ) {
            Flow flow = iter.next();
            if (Objects.equals(flow.getCredentialName(), secret.getName())) {
                continue;
            }

            iter.remove();
        }

        return list;
    }

    @Override
    public List<Flow> list(String userId, Status status) {
        List<String> flowIds = flowUserDao.findAllFlowsByUserId(userId);

        if (flowIds.isEmpty()) {
            return Collections.emptyList();
        }

        return flowDao.findAllByIdInAndStatus(flowIds, status);
    }

    @Override
    public Boolean exist(String name) {
        try {
            Flow flow = get(name);
            return flow.getStatus() != Status.PENDING;
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public Flow create(String name) {
        if (!NodePath.validate(name)) {
            String message = "Illegal flow name {0}, the length cannot over 100 and '*' ',' is not available";
            throw new ArgumentException(message, name);
        }

        String userId = sessionManager.getUserId();

        Flow flow = flowDao.findByName(name);
        if (flow != null && flow.getStatus() == Status.CONFIRMED) {
            throw new DuplicateException("Flow {0} already exists", name);
        }

        // reuse from pending list
        List<Flow> pending = flowDao.findAllByStatusAndCreatedBy(Status.PENDING, userId);
        flow = pending.size() > 0 ? pending.get(0) : new Flow();

        // set properties
        flow.setName(name);
        flow.setCreatedBy(userId);

        setupDefaultVars(flow);

        try {
            flowDao.save(flow);
            flowUserDao.create(flow.getId());
            fileManager.create(flow);

            addUsers(flow, flow.getCreatedBy());
            createFlowJobQueue(flow);

            eventManager.publish(new FlowCreatedEvent(this, flow));
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Flow {0} already exists", name);
        } catch (IOException e) {
            flowDao.delete(flow);
            flowUserDao.delete(flow.getId());
            log.error(e);
            throw new StatusException("Cannot create flow workspace");
        }

        return flow;
    }

    @Override
    public Flow confirm(String name, String gitUrl, String credential) {
        Flow flow = get(name);

        if (flow.getStatus() == Status.CONFIRMED) {
            throw new DuplicateException("Flow {0} has created", name);
        }

        if (StringHelper.hasValue(gitUrl)) {
            flow.getLocally().put(Variables.Flow.GitUrl, VarValue.of(gitUrl, VarType.GIT_URL, true));
        }

        if (StringHelper.hasValue(credential)) {
            flow.getLocally().put(Variables.Flow.GitCredential, VarValue.of(credential, VarType.STRING, true));
        }

        flow.setStatus(Status.CONFIRMED);
        flowDao.save(flow);

        eventManager.publish(new FlowConfirmedEvent(this, flow));
        return flow;
    }

    @Override
    public Flow get(String name) {
        Flow flow = flowDao.findByName(name);
        if (Objects.isNull(flow)) {
            throw new NotFoundException("Flow {0} is not found", name);
        }
        return flow;
    }

    @Override
    public Flow getById(String id) {
        Optional<Flow> optional = flowDao.findById(id);

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new NotFoundException("Invalid flow id {0}", id);
    }

    @Override
    public Flow delete(String name) {
        Flow flow = get(name);
        flowDao.delete(flow);
        flowUserDao.delete(flow.getId());

        removeFlowJobQueue(flow);
        eventManager.publish(new FlowDeletedEvent(this, flow));
        return flow;
    }

    @Override
    public void update(Flow flow) {
        flow.setUpdatedAt(Date.from(Instant.now()));
        flowDao.save(flow);
    }

    @Override
    public String setSshRsaCredential(String name, SimpleKeyPair pair) {
        Flow flow = get(name);

        String credentialName = "flow-" + flow.getName() + "-ssh-rsa";
        secretService.createRSA(credentialName, pair);

        return credentialName;
    }

    @Override
    public String setAuthCredential(String name, SimpleAuthPair keyPair) {
        Flow flow = get(name);

        String credentialName = "flow-" + flow.getName() + "-auth";
        secretService.createAuth(credentialName, keyPair);

        return credentialName;
    }

    @Override
    public void addUsers(Flow flow, String... userIds) {
        flowUserDao.insert(flow.getId(), Sets.newHashSet(userIds));
    }

    @Override
    public List<String> listUsers(Flow flow) {
        return flowUserDao.findAllUsers(flow.getId());
    }

    @Override
    public void removeUsers(Flow flow, String... userIds) {
        Set<String> idSet = Sets.newHashSet(userIds);

        if (idSet.contains(flow.getCreatedBy())) {
            throw new ArgumentException("Cannot remove user who create the flow");
        }

        if (idSet.contains(sessionManager.getUserId())) {
            throw new ArgumentException("Cannot remove current user from flow");
        }

        flowUserDao.remove(flow.getId(), idSet);
    }

    // ====================================================================
    // %% Internal events
    // ====================================================================

    @EventListener
    public void initJobQueueForFlow(ContextRefreshedEvent ignore) {
        List<Flow> all = flowDao.findAll();

        for (Flow flow : all) {
            createFlowJobQueue(flow);
        }

        eventManager.publish(new FlowInitEvent(this, all));
    }

    @EventListener
    public void deleteUserFromFlow(UserDeletedEvent event) {
        // TODO:
    }

    @EventListener
    public void onGitHookEvent(GitHookEvent event) {
        Flow flow = get(event.getFlow());

        if (event.isPingEvent()) {
            GitPingTrigger ping = (GitPingTrigger) event.getTrigger();

            Flow.WebhookStatus ws = new Flow.WebhookStatus();
            ws.setAdded(true);
            ws.setCreatedAt(ping.getCreatedAt());
            ws.setEvents(ping.getEvents());

            flow.setWebhookStatus(ws);
            update(flow);
        } else {
            Optional<Yml> optional = ymlDao.findById(flow.getId());

            if (!optional.isPresent()) {
                log.warn("No available yml for flow {}", flow.getName());
                return;
            }

            Yml yml = optional.get();
            FlowNode root = YmlParser.load(flow.getName(), yml.getRaw());

            if (!canStartJob(root, event.getTrigger())) {
                log.debug("Cannot start job since filter not matched on flow {}", flow.getName());
                return;
            }

            StringVars gitInput = event.getTrigger().toVariableMap();
            Trigger jobTrigger = event.getTrigger().toJobTrigger();

            eventManager.publish(new CreateNewJobEvent(this, flow, yml.getRaw(), jobTrigger, gitInput));
        }
    }

    // ====================================================================
    // %% Utils
    // ====================================================================

    private void setupDefaultVars(Flow flow) {
        Vars<VarValue> localVars = flow.getLocally();
        localVars.put(Variables.Flow.Name, VarValue.of(flow.getName(), VarType.STRING, false));
        localVars.put(Variables.Flow.Webhook, VarValue.of(getWebhook(flow.getName()), VarType.HTTP_URL, false));
    }

    private boolean canStartJob(FlowNode root, GitTrigger trigger) {
        TriggerFilter condition = root.getTrigger();

        if (trigger.getEvent() == GitEvent.PUSH) {
            GitPushTrigger pushTrigger = (GitPushTrigger) trigger;
            return condition.isMatchBranch(pushTrigger.getRef());
        }

        if (trigger.getEvent() == GitEvent.TAG) {
            GitPushTrigger tagTrigger = (GitPushTrigger) trigger;
            return condition.isMatchTag(tagTrigger.getRef());
        }

        return true;
    }

    private void createFlowJobQueue(Flow flow) {
        try {
            jobQueueManager.declare(flow.getQueueName(), true, 255, rabbitProperties.getJobDlExchange());
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
    }

    private void removeFlowJobQueue(Flow flow) {
        jobQueueManager.delete(flow.getQueueName());
    }

    private String getWebhook(String name) {
        return serverUrl + "/webhooks/" + name;
    }
}
