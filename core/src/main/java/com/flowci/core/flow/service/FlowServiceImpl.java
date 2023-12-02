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
import com.flowci.core.flow.dao.FlowUsersDao;
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
import com.flowci.core.secret.event.GetSecretEvent;
import com.flowci.core.user.event.UserDeletedEvent;
import com.flowci.domain.StringVars;
import com.flowci.domain.VarType;
import com.flowci.domain.VarValue;
import com.flowci.common.exception.ArgumentException;
import com.flowci.common.exception.DuplicateException;
import com.flowci.common.exception.NotFoundException;
import com.flowci.common.exception.StatusException;
import com.flowci.store.FileManager;
import com.flowci.util.StringHelper;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

/**
 * @author yang
 */
@Slf4j
@Service
@AllArgsConstructor
public class FlowServiceImpl implements FlowService {

    private final FlowDao flowDao;

    private final FlowGroupDao flowGroupDao;

    private final FlowUsersDao flowUsersDao;

    private final SessionManager sessionManager;

    private final SpringEventManager eventManager;

    private final FileManager fileManager;

    private final HttpRequestManager httpRequestManager;

    private final YmlService ymlService;

    private final CronService cronService;

    private final List<Template> templates;

    private final AppProperties appProperties;

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
        List<Flow> list = listFlowsOfCurrentUser();
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
    @Transactional
    public Flow create(String name, CreateOption option) {
        Objects.requireNonNull(option, "CreateOption is missing");
        Flow.validateName(name);

        var flow = new Flow(name);
        flow.getVars().put(Variables.Flow.Name, VarValue.of(flow.getName(), VarType.STRING, false));

        if (option.hasGroupName()) {
            var opt = flowGroupDao.findByName(option.getGroupName());
            if (opt.isEmpty()) {
                throw new NotFoundException("group {0} not found", option.getGroupName());
            }
            flow.setParentId(opt.get().getParentId());
        }

        try {
            flowDao.save(flow);
            fileManager.create(flow);

            if (!option.isBlank()) {
                var b64Content = getBase64Content(option);
                ymlService.saveYml(flow, List.of(new SimpleYml(FlowYml.DEFAULT_NAME, b64Content)));
            }


            var users = new FlowUsers(flow.getId());
            users.add(flow.getCreatedBy());
            flowUsersDao.save(users);

            eventManager.publish(new FlowCreatedEvent(this, flow));
            return flow;
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Flow {0} already exists", name);
        } catch (IOException e) {
            log.error("Unable to create flow workspace", e);
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
        flowUsersDao.deleteById(flow.getId());

        ymlService.delete(flow.getId());
        cronService.cancel(flow);

        eventManager.publish(new FlowDeletedEvent(this, flow));
    }

    @Override
    @Transactional
    public void addUsers(Flow flow, String... emails) {
        var emailSet = Sets.newHashSet(emails);
        flowUsersDao.insert(flow.getId(), emailSet);

        if (flow.hasParentId()) {
            flowUsersDao.insert(flow.getParentId(), emailSet);
        }
    }

    @Override
    public List<String> listUsers(String name) {
        Flow flow = get(name);
        Optional<FlowUsers> optional = flowUsersDao.findById(flow.getId());

        if (optional.isEmpty()) {
            throw new NotFoundException("Users of flow {0} not found", flow.getName());
        }

        return optional.get().getUsers();
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

        flowUsersDao.remove(flow.getId(), emailSet);
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

        var outEvent = new CreateNewJobEvent(this, flow, ymlService.get(flow.getId()), jobTrigger, gitInput);
        eventManager.publish(outEvent);
    }

    // ====================================================================
    // %% Utils
    // ====================================================================

    private String getBase64Content(CreateOption option) {
        if (option.hasTemplateTitle()) {
            try {
                String content = loadYmlFromTemplate(option.getTemplateTitle());
                return StringHelper.toBase64(content);
            } catch (IOException e) {
                throw new NotFoundException("Unable to load template {0} content", option.getTemplateTitle());
            }
        }

        if (option.hasRawYaml()) {
            return option.getRawYaml();
        }

        throw new NotFoundException("Yaml content or template name not found");
    }

    private List<Flow> listFlowsOfCurrentUser() {
        String email = sessionManager.getUserEmail();
        List<String> flowIds = flowUsersDao.findAllFlowsByUserEmail(email);

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
