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

package com.flowci.core.api;

import com.flowci.core.api.domain.AddStatsItem;
import com.flowci.core.api.domain.CreateJobArtifact;
import com.flowci.core.api.domain.CreateJobReport;
import com.flowci.core.api.service.OpenRestService;
import com.flowci.core.secret.domain.RSASecret;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.flow.domain.StatsCounter;
import com.flowci.core.user.domain.User;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Provides API which calling from agent plugin
 */
@RestController
@RequestMapping("/api")
public class OpenRestController {

    @Autowired
    private OpenRestService openRestService;

    @GetMapping("/credential/{name}")
    public Secret getSecret(@PathVariable String name) {
        Secret credential = openRestService.getSecret(name);
        credential.cleanDBInfo();

        if (credential instanceof RSASecret) {
            RSASecret rsa = (RSASecret) credential;
            rsa.setPublicKey(null);
        }

        return credential;
    }

    @GetMapping("/flow/{name}/users")
    public List<User> listFlowUserEmail(@PathVariable String name) {
        return openRestService.users(name);
    }

    @PostMapping("/flow/{name}/stats")
    public void addStatsItem(@PathVariable String name,
                             @Validated @RequestBody AddStatsItem body) {
        openRestService.saveStatsForFlow(name, body.getType(), StatsCounter.from(body.getData()));
    }

    @PostMapping("/flow/{name}/job/{number}/context")
    public void addJobContext(@PathVariable String name,
                              @PathVariable long number,
                              @RequestBody Map<String, String> vars) {
        openRestService.addToJobContext(name, number, vars);
    }

    @PostMapping("/flow/{name}/job/{number}/report")
    public void uploadJobReport(@PathVariable String name,
                                @PathVariable long number,
                                @Validated @RequestPart("body") CreateJobReport meta,
                                @RequestPart("file") MultipartFile file) {

        openRestService.saveJobReport(name, number, meta, file);
    }

    @PostMapping("/flow/{name}/job/{number}/artifact")
    public void uploadJobArtifact(@PathVariable String name,
                                  @PathVariable long number,
                                  @Validated @RequestPart("body") CreateJobArtifact meta,
                                  @RequestPart("file") MultipartFile file) {
        openRestService.saveJobArtifact(name, number, meta, file);
    }
}
