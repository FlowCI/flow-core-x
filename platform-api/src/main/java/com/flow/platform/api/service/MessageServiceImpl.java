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
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.api.util.CommonUtil;
import com.flow.platform.api.util.SmtpUtil;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.util.Logger;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.velocity.VelocityEngineUtils;
import sun.misc.MessageUtils;

/**
 * @author yh@firim
 */

@Service
public class MessageServiceImpl extends CurrentUser implements MessageService {

    private final static Logger LOGGER = new Logger(MessageService.class);

    @Autowired
    private MessageSettingDao messageDao;

    @Autowired
    private JobService jobService;

    @Autowired
    private VelocityEngine velocityEngine;

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
    public void sendMessage(BigInteger jobId) {
        LOGGER.traceMarker("sendMessage", "job failure send email");
        EmailSettingContent emailSettingContent = (EmailSettingContent) find(MessageType.EMAIl);
        if (emailSettingContent == null) {
            LOGGER.traceMarker("sendMessage", " sorry not found email settings");
            throw new NotFoundException("setting content not found");
        }

        Job job = jobService.find(jobId);
        Map model = new HashMap();
        model.put("job", job);
        String text = VelocityEngineUtils
            .mergeTemplateIntoString(velocityEngine, "email/failure_email.vm", model);
        System.out.println(text);
        SmtpUtil.sendEmail(emailSettingContent, "13581648716@163.com", "FlowCi Build Failure", text);
        LOGGER.traceMarker("sendMessage", "send message success");
    }
}
