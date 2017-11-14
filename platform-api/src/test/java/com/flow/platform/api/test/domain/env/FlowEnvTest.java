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

package com.flow.platform.api.test.domain.env;

import static com.flow.platform.api.envs.FlowEnvs.YmlStatusValue.ERROR;
import static com.flow.platform.api.envs.FlowEnvs.YmlStatusValue.FOUND;
import static com.flow.platform.api.envs.FlowEnvs.YmlStatusValue.GIT_CONNECTING;
import static com.flow.platform.api.envs.FlowEnvs.YmlStatusValue.GIT_LOADED;
import static com.flow.platform.api.envs.FlowEnvs.YmlStatusValue.GIT_LOADING;
import static com.flow.platform.api.envs.FlowEnvs.YmlStatusValue.NOT_FOUND;
import static com.flow.platform.api.envs.FlowEnvs.YmlStatusValue.isLoadingStatus;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class FlowEnvTest {

    @Test
    public void should_has_yml_loading_status() {
        Assert.assertEquals(false, isLoadingStatus(NOT_FOUND.value()));
        Assert.assertEquals(true, isLoadingStatus(GIT_CONNECTING.value()));
        Assert.assertEquals(true, isLoadingStatus(GIT_LOADING.value()));
        Assert.assertEquals(false, isLoadingStatus(GIT_LOADED.value()));
        Assert.assertEquals(false, isLoadingStatus(FOUND.value()));
        Assert.assertEquals(false, isLoadingStatus(ERROR.value()));
    }

    @Test
    public void should_not_be_yml_loading_when_value_is_null() {
        Assert.assertEquals(false, isLoadingStatus(null));
    }

    @Test
    public void should_not_be_yml_loading_when_value_is_others() {
        Assert.assertEquals(false, isLoadingStatus("hello"));
    }
}
