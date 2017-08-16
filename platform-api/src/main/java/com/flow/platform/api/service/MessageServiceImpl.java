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
import com.flow.platform.api.domain.EmailSetting;
import com.flow.platform.api.domain.MessageSetting;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.Logger;
import java.lang.reflect.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */

@Service(value = "messageService")
public class MessageServiceImpl implements MessageService {

    private final static Logger LOGGER = new Logger(MessageService.class);

    @Autowired
    private MessageSettingDao messageDao;

    @Override
    public MessageSetting save(MessageSetting t) {
        MessageSetting messageSetting = new MessageSetting();
        messageSetting.setContent(Jsonable.GSON_CONFIG.toJson(t));
        messageSetting.setType(t.getType());
        messageDao.save(messageSetting);
        return t;
    }

    @Override
    public MessageSetting find(String type) {
        MessageSetting messageSetting = messageDao.get(type);
        try {
            return Jsonable.GSON_CONFIG.fromJson(messageSetting.getContent(),
                convertClazz(messageSetting.getType()));
        } catch (Throwable throwable) {
            LOGGER.warn(String.format("not found class, exception - %s", throwable));
        }
        return null;
    }

    @Override
    public MessageSetting update(MessageSetting t) {
        MessageSetting messageSetting = messageDao.get(t.getType());
        messageSetting.setContent(Jsonable.GSON_CONFIG.toJson(t));
        messageDao.update(messageSetting);
        return messageSetting;
    }

    @Override
    public void delete(MessageSetting t) {
        MessageSetting messageSetting = messageDao.get(t.getType());
        messageDao.delete(messageSetting);
    }

    private Type convertClazz(String type){
        switch (type){
            case "EmailSetting" :
                return EmailSetting.class;
        }
        return null;
    }
}
