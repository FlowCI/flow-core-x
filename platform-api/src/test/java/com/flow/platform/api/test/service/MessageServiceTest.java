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

import com.flow.platform.api.domain.EmailSetting;
import com.flow.platform.api.service.MessageService;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * @author yh@firim
 */
public class MessageServiceTest extends TestBase {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void should_save_and_update_success(){
        EmailSetting emailSetting = new EmailSetting("smtp.qq.com", 25, "admin@qq.com");
        messageService.save(emailSetting);
        EmailSetting setting = (EmailSetting) messageService.find("EmailSetting");
        Assert.assertNotNull(setting);
        Assert.assertNotNull(setting.getSmtpUrl());
        Assert.assertNotNull(setting.getSmtpPort());
        Assert.assertNotNull(setting.getSender());

        setting.setSender("admin@163.com");
        messageService.update(setting);

        EmailSetting settingt = (EmailSetting) messageService.find("EmailSetting");
        Assert.assertEquals("admin@163.com", settingt.getSender());
    }

    @Test
    public void should_delete_success(){
        EmailSetting emailSetting = new EmailSetting("smtp.qq.com", 25, "admin@qq.com");
        messageService.save(emailSetting);
        EmailSetting setting = (EmailSetting) messageService.find("EmailSetting");
        Assert.assertNotNull(setting);
        Assert.assertNotNull(setting.getSmtpUrl());
        Assert.assertNotNull(setting.getSmtpPort());
        Assert.assertNotNull(setting.getSender());

        messageService.delete(emailSetting);

        EmailSetting settingT = (EmailSetting) messageService.find("EmailSetting");
        Assert.assertNull(settingT);
    }
}
