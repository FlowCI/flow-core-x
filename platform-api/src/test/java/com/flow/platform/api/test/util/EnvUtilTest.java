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

package com.flow.platform.api.test.util;

import com.flow.platform.api.domain.Node;
import com.flow.platform.api.util.EnvUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author yang
 */
public class EnvUtilTest {

    private Node[] nodes = new Node[2];

    @Before
    public void init() {
        // given:
        Node source = new Node();
        source.getEnvs().put("FLOW_1", "source 1");
        source.getEnvs().put("FLOW_2", "source 2");
        nodes[0] = source;

        Node target = new Node();
        target.getEnvs().put("FLOW_2", "target 2");
        target.getEnvs().put("FLOW_3", "target 3");
        nodes[1] = target;
    }

    @Test
    public void should_merge_env_without_overwrite() {
        // given:
        Node source = nodes[0];
        Node target = nodes[1];

        // when: merget without overwrite
        EnvUtil.merge(source, target, false);

        // then:
        Assert.assertEquals("source 1", target.getEnvs().get("FLOW_1"));
        Assert.assertEquals("target 2", target.getEnvs().get("FLOW_2"));
        Assert.assertEquals("target 3", target.getEnvs().get("FLOW_3"));
    }

    @Test
    public void should_merge_env_with_overwrite() {
        // given:
        Node source = nodes[0];
        Node target = nodes[1];

        // when: merget without overwrite
        EnvUtil.merge(source, target, true);

        // then:
        Assert.assertEquals("source 1", target.getEnvs().get("FLOW_1"));
        Assert.assertEquals("source 2", target.getEnvs().get("FLOW_2"));
        Assert.assertEquals("target 3", target.getEnvs().get("FLOW_3"));
    }
}
