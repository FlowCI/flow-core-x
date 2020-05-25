/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.test.common;

import com.flowci.core.common.manager.VarManager;
import com.flowci.domain.Input;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.VarType;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class VarManagerTest extends SpringScenario {

    @Autowired
    private VarManager varManager;

    @Test
    public void should_verify_integer() {
        Input v = new Input()
            .setName("test")
            .setAlias("test")
            .setRequired(true)
            .setType(VarType.INT);

        Assert.assertTrue(varManager.verify(v, "123"));
        Assert.assertFalse(varManager.verify(v, ""));
        Assert.assertFalse(varManager.verify(v, "123A"));
    }

    @Test
    public void should_verify_email() {
        Input in = new Input()
            .setName("test")
            .setAlias("test")
            .setRequired(false)
            .setType(VarType.EMAIL);


        Assert.assertTrue(varManager.verify(in, "hi@flow.ci"));
        Assert.assertTrue(varManager.verify(in, ""));
        Assert.assertFalse(varManager.verify(in, "123A"));
    }

    @Test
    public void should_verify_git_url() {
        Input in = new Input()
            .setName("test")
            .setAlias("test")
            .setRequired(false)
            .setType(VarType.GIT_URL);


        Assert.assertTrue(varManager.verify(in, "git@github.com:FlowCI/flow-platform-x.git"));
        Assert.assertTrue(varManager.verify(in, ""));

        Assert.assertFalse(varManager.verify(in, "123A"));
        Assert.assertFalse(varManager.verify(in, "git@github.com:FlowCI"));
        Assert.assertFalse(varManager.verify(in, "git@github.com"));
        Assert.assertFalse(varManager.verify(in, "http://github.com"));
    }

    @Test
    public void should_verify_web_url() {
        Input in = new Input()
            .setName("test")
            .setAlias("test")
            .setRequired(false)
            .setType(VarType.HTTP_URL);


        Assert.assertTrue(varManager.verify(in, "http://www.google.com"));
        Assert.assertTrue(varManager.verify(in, "http://github.com"));
        Assert.assertTrue(varManager.verify(in, ""));

        Assert.assertFalse(varManager.verify(in, "123A"));
        Assert.assertFalse(varManager.verify(in, "git@github.com:FlowCI"));
        Assert.assertFalse(varManager.verify(in, "git@github.com"));
    }
}
