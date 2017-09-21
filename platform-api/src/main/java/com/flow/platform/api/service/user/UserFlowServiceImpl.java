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
package com.flow.platform.api.service.user;

import com.flow.platform.api.dao.FlowDao;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.dao.user.UserFlowDao;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.domain.user.UserFlow;
import com.flow.platform.api.domain.user.UserFlowKey;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lhl
 */
@Service
@Transactional
public class UserFlowServiceImpl implements UserFlowService{

    @Autowired
    private UserFlowDao userFlowDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private FlowDao flowDao;

    @Override
    public List<User> list(String flowPath) {
        List<String> userEmails = userFlowDao.listByFlowPath(flowPath);
        if (userEmails.isEmpty()) {
            return new ArrayList<>(0);
        }
        return userDao.list(userEmails);
    }

    @Override
    public List<Flow> list(User user) {
        List<String> flowPaths = userFlowDao.listByEmail(user.getEmail());
        if (flowPaths.isEmpty()) {
            return new ArrayList<>(0);
        }
        return flowDao.list(flowPaths);
    }

    @Override
    public void assign(User user, Flow flow) {
        UserFlow userFlow = new UserFlow(flow.getPath(), user.getEmail());
        userFlowDao.save(userFlow);
    }

    @Override
    public void unAssign(User user) {
        userFlowDao.deleteByEmail(user.getEmail());
    }

    @Override
    public void unAssign(User user, Flow flow) {
        UserFlow userFlow = userFlowDao.get(new UserFlowKey(flow.getPath(), user.getEmail()));
        if (userFlow != null) {
            userFlowDao.delete(userFlow);
        }
    }

}
