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

package com.flowci.core.test.plugin;

import com.flowci.core.plugin.domain.Input;
import com.flowci.domain.VarType;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class VariableTest {

    @Test
    public void should_verify_integer() {
        Input v = new Input()
            .setName("test")
            .setAlias("test")
            .setRequired(true)
            .setType(VarType.INT);

        Assert.assertTrue(v.verify("123"));

        Assert.assertFalse(v.verify(""));
        Assert.assertFalse(v.verify("123A"));
    }

    @Test
    public void should_verify_email() {
        Input v = new Input()
            .setName("test")
            .setAlias("test")
            .setRequired(false)
            .setType(VarType.EMAIL);


        Assert.assertTrue(v.verify("hi@flow.ci"));
        Assert.assertTrue(v.verify(""));

        Assert.assertFalse(v.verify("123A"));
    }

    @Test
    public void should_verify_git_url() {
        Input v = new Input()
            .setName("test")
            .setAlias("test")
            .setRequired(false)
            .setType(VarType.GIT_URL);


        Assert.assertTrue(v.verify("git@github.com:FlowCI/flow-platform-x.git"));
        Assert.assertTrue(v.verify(""));

        Assert.assertFalse(v.verify("123A"));
        Assert.assertFalse(v.verify("git@github.com:FlowCI"));
        Assert.assertFalse(v.verify("git@github.com"));
        Assert.assertFalse(v.verify("http://github.com"));
    }

    @Test
    public void should_verify_web_url() {
        Input v = new Input()
            .setName("test")
            .setAlias("test")
            .setRequired(false)
            .setType(VarType.HTTP_URL);


        Assert.assertTrue(v.verify("http://www.google.com"));
        Assert.assertTrue(v.verify("http://github.com"));
        Assert.assertTrue(v.verify(""));

        Assert.assertFalse(v.verify("123A"));
        Assert.assertFalse(v.verify("git@github.com:FlowCI"));
        Assert.assertFalse(v.verify("git@github.com"));
    }
}
