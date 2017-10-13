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

package com.flow.platform.api.service;

import com.flow.platform.api.dao.MessageSettingDao;
import com.flow.platform.api.domain.EmailSettingContent;
import com.flow.platform.api.domain.MessageSetting;
import com.flow.platform.api.domain.MessageType;
import com.flow.platform.api.domain.SettingContent;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.api.service.user.UserFlowService;
import com.flow.platform.api.util.SmtpUtil;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.util.ExceptionUtil;
import com.flow.platform.util.Logger;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.util.List;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yh@firim
 */

@Service
@Transactional
public class MessageServiceImpl extends CurrentUser implements MessageService {

    private final static Logger LOGGER = new Logger(MessageService.class);

    private final static String FAILURE_TEMPLATE_SUBJECT = "FlowCi Build Failure";

    private final static String SUCCESS_TEMPLATE_SUBJECT = "FlowCi Build Success";

    @Autowired
    private MessageSettingDao messageDao;

    @Autowired
    private JobService jobService;

    @Autowired
    private VelocityEngine velocityEngine;

    @Autowired
    private UserFlowService userFlowService;

    @Value("${domain.web}")
    private String webDomain;

    @Override
    public SettingContent save(SettingContent t) {
        MessageSetting messageSetting = new MessageSetting(t, ZonedDateTime.now(), ZonedDateTime.now());
        messageSetting.setCreatedBy(currentUser().getEmail());
        if (findSettingByType(t.getType()) == null) {
            messageDao.save(messageSetting);
        } else {
            update(t);
        }
        return t;
    }

    @Override
    public SettingContent find(MessageType type) {
        if (findSettingByType(type) == null) {
            return null;
        }

        return findSettingByType(type).getContent();
    }

    @Override
    public void delete(SettingContent t) {
        MessageSetting messageSetting = findSettingByType(t.getType());
        messageDao.delete(messageSetting);
    }

    @Override
    public SettingContent update(SettingContent t) {
        MessageSetting messageSetting = findSettingByType(t.getType());

        //if not exist to save
        if (messageSetting == null) {
            return save(t);
        }
        messageSetting.setContent(t);
        messageDao.update(messageSetting);
        return t;
    }

    @Override
    public Boolean authEmailSetting(EmailSettingContent emailSetting) {
        return SmtpUtil.authentication(emailSetting);
    }

    private List<MessageSetting> listMsg() {
        return messageDao.list();
    }

    private MessageSetting findSettingByType(MessageType type) {
        for (MessageSetting messageSetting : listMsg()) {
            if (messageSetting.getContent().getType() == type) {
                return messageSetting;
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public void sendMessage(Job job, JobStatus jobStatus) {
        LOGGER.traceMarker("sendMessage", "Start to send job %s email", jobStatus);
        EmailSettingContent emailSettingContent = (EmailSettingContent) find(MessageType.EMAIl);

        if (emailSettingContent == null) {
            LOGGER.warnMarker("sendMessage", "Email settings not found");
            return;
        }

        String text = buildEmailTemplate(job, jobStatus);
        bindModelAndSendMessage(job, emailSettingContent, text, jobStatus);
    }

    /**
     * bind model and send message all member
     */
    private void bindModelAndSendMessage(Job job, EmailSettingContent emailSettingContent, String text,
                                         JobStatus jobStatus) {

        try {
            // send email to creator
            SmtpUtil.sendEmail(emailSettingContent, job.getCreatedBy(), getEmailSubject(jobStatus), text);
            LOGGER.traceMarker("sendMessage", "send message to %s success", job.getCreatedBy());

            // send email to member of this flow
            List<User> members = userFlowService.list(job.getNodePath());
            for (User member : members) {
                SmtpUtil.sendEmail(emailSettingContent, member.getEmail(), getEmailSubject(jobStatus), text);
                LOGGER.traceMarker("sendMessage", "send message to %s success", member.getEmail());
            }

            LOGGER.traceMarker("sendMessage", "send message to all member success");
        } catch (Throwable e) {
            LOGGER.traceMarker("sendMessage", "send message to all member error : %s",
                ExceptionUtil.findRootCause(e).getMessage());
        }
    }

    private String getEmailSubject(JobStatus jobStatus) {
        if (Job.FAILURE_STATUS.contains(jobStatus)) {
            return FAILURE_TEMPLATE_SUBJECT;
        }

        return SUCCESS_TEMPLATE_SUBJECT;
    }


    private String buildEmailTemplate(Job job, JobStatus jobStatus) {
        Template template = null;
        try {
            if (Job.SUCCESS_STATUS.contains(jobStatus)) {
                template = velocityEngine.getTemplate("email/success_email.vm");
            }

            if (Job.FAILURE_STATUS.contains(jobStatus)) {
                template = velocityEngine.getTemplate("email/failure_email.vm");
            }

            VelocityContext velocityContext = new VelocityContext();
            velocityContext.put("job", job);
            velocityContext
                .put("detailUrl", String.format("%s/flows/%s/jobs/%s", webDomain, job.getNodeName(), job.getNumber()));
            StringWriter stringWriter = new StringWriter();
            template.merge(velocityContext, stringWriter);
            return stringWriter.toString();

        } catch (Throwable e) {
            LOGGER.warn("sendMessage", "send message to all member error : %s",
                ExceptionUtil.findRootCause(e).getMessage());
        }
        return null;
    }
}
