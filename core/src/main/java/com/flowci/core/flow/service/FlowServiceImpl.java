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

import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.manager.HttpRequestManager;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.FlowGroupDao;
import com.flowci.core.flow.dao.FlowUserDao;
import com.flowci.core.flow.domain.*;
import com.flowci.core.flow.event.FlowCreatedEvent;
import com.flowci.core.flow.event.FlowDeletedEvent;
import com.flowci.core.flow.event.FlowInitEvent;
import com.flowci.core.git.domain.GitPingTrigger;
import com.flowci.core.git.domain.GitTrigger;
import com.flowci.core.git.event.GitHookEvent;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.CreateNewJobEvent;
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
import com.flowci.util.StringHelper;
import com.google.common.collect.Sets;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private FlowDao flowDao;

    @Autowired
    private FlowGroupDao flowGroupDao;

    @Autowired
    private FlowUserDao flowUserDao;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private SpringEventManager eventManager;

    @Qualifier("fileManager")
    @Autowired
    private FileManager fileManager;

    @Autowired
    private HttpRequestManager httpRequestManager;

    @Autowired
    private YmlService ymlService;

    @Autowired
    private CronService cronService;

    @Autowired
    private List<Template> templates;

    @Autowired
    private AppProperties appProperties;

    @EventListener(ContextRefreshedEvent.class)
    public void initFlows() {
        eventManager.publish(new FlowInitEvent(this, flowDao.findAll()));
    }

    // ====================================================================
    // %% Public function
    // ====================================================================

    @Override
    public List<Flow> listByCredential(String secretName) {
        GetSecretEvent event = eventManager.publish(new GetSecretEvent(this, secretName));
        if (event.hasError()) {
            throw event.getError();
        }

        Secret secret = event.getFetched();
        List<Flow> list = list();
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
    public Flow create(String name, CreateOption option) {
        Flow.validateName(name);
        var email = sessionManager.getUserEmail();

        var flow = new Flow(name);
        flow.getVars().put(Variables.Flow.Name, VarValue.of(flow.getName(), VarType.STRING, false));

        if (option.hasGroupName()) {
            var opt = flowGroupDao.findByName(option.getGroupName());
            if (opt.isEmpty()) {
                throw new NotFoundException("group {0} not found", option.getGroupName());
            }
            flow.setParentId(opt.get().getParentId());
        }

        String template = StringHelper.EMPTY;
        if (option.hasTemplateTitle()) {
            try {
                template = loadYmlFromTemplate(option.getTemplateTitle());
            } catch (IOException e) {
                throw new NotFoundException("Unable to load template {0} content", option.getTemplateTitle());
            }
        }

        try {
            flowDao.save(flow);
            fileManager.create(flow);
            ymlService.saveYml(flow, Yml.DEFAULT_NAME, template);

            flowUserDao.create(flow.getId());
            addUsers(flow, flow.getUpdatedBy());

            eventManager.publish(new FlowCreatedEvent(this, flow));
            return flow;
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Flow {0} already exists", name);
        } catch (IOException e) {
            flowDao.delete(flow);
            log.error(e);
            throw new StatusException("Cannot create flow workspace");
        }
    }

    @Override
    public Flow get(String name) {
        Optional<Flow> optional = flowDao.findByName(name);

        if (optional.isEmpty()) {
            throw new NotFoundException("Flow {0} is not found", name);
        }
        return optional.get();
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
    public void delete(Flow flow) {
        flowDao.delete(flow);
        flowUserDao.delete(flow.getId());

        ymlService.delete(flow.getId());
        cronService.cancel(flow);

        eventManager.publish(new FlowDeletedEvent(this, flow));
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
        var emailSet = Sets.newHashSet(emails);
        flowUserDao.insert(flow.getId(), emailSet);

        if (flow.hasParentId()) {
            flowUserDao.insert(flow.getParentId(), emailSet);
        }
    }

    @Override
    public List<String> listUsers(String name) {
        Flow flow = get(name);
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
    public void deleteUserFromFlow(UserDeletedEvent event) {
        // TODO:
    }

    @EventListener
    public void onGitHookEvent(GitHookEvent event) {
        GitTrigger trigger = event.getTrigger();
        Optional<Flow> optional = flowDao.findByName(event.getFlow());
        if (optional.isEmpty()) {
            log.warn("The flow {} doesn't exist", event.getFlow());
            return;
        }

        var flow = optional.get();
        if (event.isPingEvent()) {
            GitPingTrigger ping = (GitPingTrigger) trigger;

            WebhookStatus ws = new WebhookStatus();
            ws.setAdded(true);
            ws.setCreatedAt(ping.getCreatedAt());
            ws.setEvents(ping.getEvents());

            flow.setWebhookStatus(ws);
            flowDao.save(flow);
            return;
        }

        if (trigger.isSkip()) {
            log.info("Ignore git trigger {} since skip message", trigger);
            return;
        }

        StringVars gitInput = trigger.toVariableMap();
        Job.Trigger jobTrigger = trigger.toJobTrigger();

        Yml yml = ymlService.getYml(flow.getId(), Yml.DEFAULT_NAME);
        eventManager.publish(new CreateNewJobEvent(this, flow, yml.getRaw(), jobTrigger, gitInput));
    }


    // ====================================================================
    // %% Utils
    // ====================================================================

    private List<Flow> list() {
        String email = sessionManager.getUserEmail();
        List<String> flowIds = flowUserDao.findAllFlowsByUserEmail(email);

        if (flowIds.isEmpty()) {
            return Collections.emptyList();
        }

        return flowDao.findAllByIdIn(flowIds);
    }

    private String loadYmlFromTemplate(String title) throws IOException {
        for (Template t : templates) {
            if (Objects.equals(t.getTitle(), title)) {
                String source = t.getSourceWithDomain(appProperties.getResourceDomain());
                return httpRequestManager.get(source);
            }
        }
        throw new NotFoundException("Unable to load template {0} content", title);
    }
}
