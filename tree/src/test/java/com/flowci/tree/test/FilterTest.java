/*
 * Copyright 2018 flow.ci
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

package com.flowci.tree.test;

import com.flowci.tree.TriggerFilter;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class FilterTest {

    @Test
    public void should_match_branches_regular_expression() {
        TriggerFilter condition = new TriggerFilter();
        condition.getBranch().add("feature/*");
        condition.getBranch().add("personal/.+");

        Assert.assertTrue(condition.isMatchBranch("feature/fb_123"));
        Assert.assertTrue(condition.isMatchBranch("personal/my-branch"));
    }

    @Test
    public void should_available_if_condition_defined() {
        TriggerFilter condition = new TriggerFilter();
        condition.getBranch().add("feature/*");
        Assert.assertTrue(condition.available());
    }

    @Test
    public void should_not_available_if_no_condition_defined() {
        TriggerFilter condition = new TriggerFilter();
        Assert.assertFalse(condition.available());
    }
}
