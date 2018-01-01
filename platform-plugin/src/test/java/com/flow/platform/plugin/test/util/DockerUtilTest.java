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

package com.flow.platform.plugin.test.util;

import com.flow.platform.plugin.test.TestBase;
import com.flow.platform.plugin.util.docker.Docker;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class DockerUtilTest extends TestBase {

    @Test
    @Ignore
    public void should_pull_success() {
        Docker docker = new Docker();
        String image = "flowci/plugin-environment";
        docker.pull(image);
        Path path = Paths.get("/Users/firim/workspace/git-clone");
        docker.runBuild(image, "mvn clean install -DskipTests=true", path);
        docker.close();
    }
}
