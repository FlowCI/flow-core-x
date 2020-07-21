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

import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.FlowUserDao;
import com.flowci.core.flow.domain.ConfirmOption;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Flow.Status;
import com.flowci.core.flow.event.FlowCreatedEvent;
import com.flowci.core.flow.event.FlowDeletedEvent;
import com.flowci.core.flow.event.FlowInitEvent;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.event.CreateAuthEvent;
import com.flowci.core.secret.event.CreateRsaEvent;
import com.flowci.core.secret.event.GetSecretEvent;
import com.flowci.core.user.event.UserDeletedEvent;
import com.flowci.domain.*;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.store.FileManager;
import com.flowci.util.ObjectsHelper;
import com.google.common.collect.Sets;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    private FlowDao flowDao;

    @Autowired
    private FlowUserDao flowUserDao;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private YmlService ymlService;

    @Autowired
    private CronService cronService;

    // ====================================================================
    // %% Public function
    // ====================================================================

    @Override
    public List<Flow> list(Status status) {
        if (sessionManager.get().isAdmin()) {
            return flowDao.findAll();
        }

        String email = sessionManager.getUserEmail();
        List<String> flowIds = flowUserDao.findAllFlowsByUserEmail(email);

        if (flowIds.isEmpty()) {
            return Collections.emptyList();
        }

        return flowDao.findAllByIdInAndStatus(flowIds, status);
    }

    @Override
    public List<Flow> listByCredential(String secretName) {
        GetSecretEvent event = eventManager.publish(new GetSecretEvent(this, secretName));
        if (event.hasError()) {
            throw event.getError();
        }

        Secret secret = event.getFetched();
        List<Flow> list = list(Status.CONFIRMED);
        Iterator<Flow> iter = list.iterator();

        while (iter.hasNext()) {
            Flow flow = iter.next();
            if (Objects.equals(flow.getCredentialName(), secret.getName())) {
                continue;
            }

            iter.remove();
        }

        return list;
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
        Flow.validateName(name);
        String email = sessionManager.getUserEmail();

        Flow flow = flowDao.findByName(name);
        if (flow != null && flow.getStatus() == Status.CONFIRMED) {
            throw new DuplicateException("Flow {0} already exists", name);
        }

        // reuse from pending list
        List<Flow> pending = flowDao.findAllByStatusAndCreatedBy(Status.PENDING, email);
        flow = pending.size() > 0 ? pending.get(0) : new Flow();

        // set properties
        flow.setName(name);
        flow.setCreatedBy(email);

        setupDefaultVars(flow);

        try {
            flowDao.save(flow);
            flowUserDao.create(flow.getId());
            fileManager.create(flow);

            addUsers(flow, flow.getCreatedBy());
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
    public Flow confirm(String name, ConfirmOption option) {
        Flow flow = get(name);

        if (flow.getStatus() == Status.CONFIRMED) {
            throw new DuplicateException("Flow {0} has created", name);
        }

        Vars<VarValue> localVars = flow.getLocally();

        if (option.hasGitUrl()) {
            localVars.put(Variables.Flow.GitUrl, VarValue.of(option.getGitUrl(), VarType.GIT_URL, true));
        }

        if (option.hasSecret()) {
            localVars.put(Variables.Flow.GitCredential, VarValue.of(option.getSecret(), VarType.STRING, true));
        }

        flow.setStatus(Status.CONFIRMED);

        // flow will be saved in saveYml
        if (option.hasYml()) {
            ymlService.saveYml(flow, option.getYaml());
        }

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

        ymlService.delete(flow);
        cronService.cancel(flow);

        eventManager.publish(new FlowDeletedEvent(this, flow));
        return flow;
    }

    @Override
    public String setSshRsaCredential(String name, SimpleKeyPair pair) {
        Flow flow = get(name);

        String secretName = "flow-" + flow.getName() + "-ssh-rsa";
        CreateRsaEvent event = eventManager.publish(new CreateRsaEvent(this, secretName, pair));

        ObjectsHelper.throwIfNotNull(event.getErr());
        return secretName;
    }

    @Override
    public String setAuthCredential(String name, SimpleAuthPair keyPair) {
        Flow flow = get(name);

        String secretName = "flow-" + flow.getName() + "-auth";
        CreateAuthEvent event = eventManager.publish(new CreateAuthEvent(this, secretName, keyPair));

        ObjectsHelper.throwIfNotNull(event.getErr());
        return secretName;
    }

    @Override
    public void addUsers(Flow flow, String... emails) {
        flowUserDao.insert(flow.getId(), Sets.newHashSet(emails));
    }

    @Override
    public List<String> listUsers(Flow flow) {
        return flowUserDao.findAllUsers(flow.getId());
    }

    @Override
    public void removeUsers(Flow flow, String... emails) {
        Set<String> emailSet = Sets.newHashSet(emails);

        if (emailSet.contains(flow.getCreatedBy())) {
            throw new ArgumentException("Cannot remove user who create the flow");
        }

        if (emailSet.contains(sessionManager.getUserEmail())) {
            throw new ArgumentException("Cannot remove current user from flow");
        }

        flowUserDao.remove(flow.getId(), emailSet);
    }

    // ====================================================================
    // %% Internal events
    // ====================================================================

    @EventListener
    public void initJobQueueForFlow(ContextRefreshedEvent ignore) {
        List<Flow> all = flowDao.findAll();
        eventManager.publish(new FlowInitEvent(this, all));
    }

    @EventListener
    public void deleteUserFromFlow(UserDeletedEvent event) {
        // TODO:
    }

    // ====================================================================
    // %% Utils
    // ====================================================================

    private void setupDefaultVars(Flow flow) {
        Vars<VarValue> localVars = flow.getLocally();
        localVars.put(Variables.Flow.Name, VarValue.of(flow.getName(), VarType.STRING, false));
        localVars.put(Variables.Flow.Webhook, VarValue.of(getWebhook(flow.getName()), VarType.HTTP_URL, false));
    }

    private String getWebhook(String name) {
        return serverUrl + "/webhooks/" + name;
    }
}
