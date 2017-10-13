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

import com.flow.platform.api.domain.EmailSettingContent;
import com.flow.platform.api.domain.MessageType;
import com.flow.platform.api.service.MessageService;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class MessageServiceTest extends TestBase {

    @Autowired
    private MessageService messageService;

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

        EmailSettingContent settings = (EmailSettingContent) messageService.find(MessageType.EMAIl);
        Assert.assertEquals("admin@qq.com", settings.getSender());
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
        Assert.assertNull(messageService.find(MessageType.EMAIl));
    }
}
