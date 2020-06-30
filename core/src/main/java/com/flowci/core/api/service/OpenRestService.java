/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.api.service;

import com.flowci.core.api.domain.CreateJobArtifact;
import com.flowci.core.api.domain.CreateJobReport;
import com.flowci.core.config.domain.Config;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.flow.domain.StatsCounter;
import com.flowci.core.user.domain.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.data.util.Pair;
import org.springframework.web.multipart.MultipartFile;

public interface OpenRestService {

    /**
     * Get credential data by name
     */
    Secret getSecret(String name);

    /**
     * Get config by name
     */
    Config getConfig(String name);

    /**
     * Get file in config
     *
     * @return key as file name, value is resource
     */
    Pair<String, Resource> getResource(Config config);

    /**
     * Save statistic data for flow
     */
    void saveStatsForFlow(String flowName, String statsType, StatsCounter counter);

    /**
     * Save uploaded job report with file
     */
    void saveJobReport(String flowName, long buildNumber, CreateJobReport meta, MultipartFile file);

    /**
     * Save uploaded job artifact
     */
    void saveJobArtifact(String flowName, long buildNumber, CreateJobArtifact meta, MultipartFile file);

    /**
     * Add env vars to job context
     */
    void addToJobContext(String flowName, long buildNumber, Map<String, String> vars);

    /**
     * List email of all flow users
     */
    List<User> users(String flowName);
}

