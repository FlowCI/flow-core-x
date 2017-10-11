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

package com.flow.platform.api.test.service;

import static com.flow.platform.api.domain.envs.FlowEnvs.FLOW_STATUS;

import com.flow.platform.api.domain.EmailSettingContent;
import com.flow.platform.api.domain.MessageType;
import com.flow.platform.api.domain.envs.FlowEnvs.StatusValue;
import com.flow.platform.api.domain.envs.JobEnvs;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.job.NodeTag;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.service.MessageService;
import com.flow.platform.api.service.job.JobNodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.CommonUtil;
import com.flow.platform.api.util.EnvUtil;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.util.git.model.GitEventType;
import com.google.common.base.Strings;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yh@firim
 */
public class MessageServiceTest extends TestBase {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JobNodeService jobNodeService;

    @Test
    public void should_save_and_update_success() {
        EmailSettingContent emailSetting = new EmailSettingContent("smtp.qq.com", 25, "admin@qq.com");
        messageService.save(emailSetting);
        EmailSettingContent setting = (EmailSettingContent) messageService.find(MessageType.EMAIl);
        Assert.assertNotNull(setting);
        Assert.assertNotNull(setting.getSmtpUrl());
        Assert.assertNotNull(setting.getSmtpPort());
        Assert.assertNotNull(setting.getSender());

        setting.setSender("admin@163.com");
        messageService.update(setting);

        EmailSettingContent settingt = (EmailSettingContent) messageService.find(MessageType.EMAIl);
        Assert.assertEquals("admin@163.com", settingt.getSender());
    }

    @Test
    public void should_delete_success() {
        EmailSettingContent emailSetting = new EmailSettingContent("smtp.qq.com", 25, "admin@qq.com");
        messageService.save(emailSetting);
        EmailSettingContent setting = (EmailSettingContent) messageService.find(MessageType.EMAIl);
        Assert.assertNotNull(setting);
        Assert.assertNotNull(setting.getSmtpUrl());
        Assert.assertNotNull(setting.getSmtpPort());
        Assert.assertNotNull(setting.getSender());

        messageService.delete(emailSetting);

        try {
            EmailSettingContent settingT = (EmailSettingContent) messageService.find(MessageType.EMAIl);
        } catch (NotFoundException e) {
        }
    }

    @Test
    public void should_send_message_success() throws IOException {
        stubDemo();
        Node rootForFlow = createRootFlow("flowTest", "demo_flow1.yaml");
        Job job = createMockJob(rootForFlow.getPath());

        EmailSettingContent emailSetting = new EmailSettingContent("smtp.163.com", 25, "liyunhehappy6@163.com");
        messageService.save(emailSetting);

        messageService.sendMessage(job.getId());
    }


    private Job createMockJob(String nodePath) {
        Job job = jobService.createJob(nodePath, GitEventType.TAG, null, mockUser);
        Assert.assertNotNull(job.getId());
        Assert.assertNotNull(job.getSessionId());
        Assert.assertNotNull(job.getNumber());
        Assert.assertEquals(mockUser.getEmail(), job.getCreatedBy());
        Assert.assertEquals(JobStatus.SESSION_CREATING, job.getStatus());

        Assert.assertEquals(job.getNumber().toString(), job.getEnv(JobEnvs.FLOW_JOB_BUILD_NUMBER));

        // verify root node result for job
        NodeResult rootResult = job.getRootResult();
        Assert.assertNotNull(rootResult);
        Assert.assertEquals(NodeTag.FLOW, rootResult.getNodeTag());
        Assert.assertNotNull(rootResult.getOutputs());
        Assert.assertEquals(NodeStatus.PENDING, rootResult.getStatus());

        NodeTree nodeTree = jobNodeService.get(job);

        // verify child node result list
        List<NodeResult> childrenResult = job.getChildrenResult();
        Assert.assertNotNull(childrenResult);
        Assert.assertEquals(nodeTree.childrenSize(), childrenResult.size());

        return job;
    }
}
