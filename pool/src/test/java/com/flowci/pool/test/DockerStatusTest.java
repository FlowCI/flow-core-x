/*
 * Copyright 2020 flow.ci
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

package com.flowci.pool.test;

import org.junit.Assert;
import org.junit.Test;

import static com.flowci.pool.domain.DockerStatus.*;

public class DockerStatusTest {

    @Test
    public void should_convert_to_single_state_string() {
        Assert.assertEquals(Paused, toStateString("Up 1 day (Paused)"));
        Assert.assertEquals(Restarting, toStateString("Restarting (123) 1 day ago"));
        Assert.assertEquals(Running, toStateString("Up 1 day"));
        Assert.assertEquals(Removing, toStateString("Removal In Progress"));
        Assert.assertEquals(Dead, toStateString("Dead"));
        Assert.assertEquals(Created, toStateString("Created"));
        Assert.assertEquals(Exited, toStateString("Exited (137) 5 days ago"));
    }

}
