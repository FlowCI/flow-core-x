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
import com.flowci.core.config.domain.Config;
import com.flowci.core.flow.domain.MatrixCounter;
import com.flowci.core.job.domain.JobCache;
import com.flowci.core.job.service.CacheService;
import com.flowci.core.job.service.LoggingService;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.user.domain.User;
import com.flowci.common.exception.ArgumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Provides API which calling from agent plugin
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class OpenRestController {

    @Autowired
    private OpenRestService openRestService;

    @Autowired
    private LoggingService loggingService;

    @Autowired
    private CacheService cacheService;

    @GetMapping("/secret/{name}")
    public Secret getSecret(@PathVariable String name) {
        return openRestService.getSecret(name);
    }

    @GetMapping("/secret/{name}/download/{file:.+}")
    public ResponseEntity<Resource> downloadSecretFile(@PathVariable String name,
                                                       @PathVariable String file) {
        Secret secret = openRestService.getSecret(name);
        Resource resource = openRestService.getResource(secret, file);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                .body(resource);
    }

    @GetMapping("/config/{name}")
    public Config getConfig(@PathVariable String name) {
        return openRestService.getConfig(name);
    }

    @GetMapping("/flow/{name}/users")
    public List<User> listFlowUserEmail(@PathVariable String name) {
        return openRestService.users(name);
    }

    @PostMapping("/flow/{name}/stats")
    public void addStatsItem(@PathVariable String name,
                             @Validated @RequestBody AddStatsItem body) {
        openRestService.saveStatsForFlow(name, body.getType(), MatrixCounter.from(body.getData()));
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

    @PostMapping("/logs/upload")
    public void upload(@RequestPart("file") MultipartFile file) {
        try (InputStream stream = file.getInputStream()) {
            loggingService.save(file.getOriginalFilename(), stream);
        } catch (IOException e) {
            log.warn("Unable to save log, cause {}", e.getMessage());
        }
    }

    @PostMapping("/cache/{jobId}/{key}/{os}")
    public void putCache(@PathVariable String jobId,
                         @PathVariable String key,
                         @PathVariable String os,
                         @RequestParam("files") MultipartFile[] files) {
        if (files.length == 0) {
            throw new ArgumentException("the cached files are empty");
        }

        cacheService.put(jobId, key, os, files);
    }

    @GetMapping("/cache/{jobId}/{key}")
    public JobCache getCache(@PathVariable String jobId, @PathVariable String key) {
        return cacheService.get(jobId, key);
    }

    @GetMapping("/cache/{cacheId}")
    public ResponseEntity<Resource> downloadCache(@PathVariable String cacheId, @RequestParam String file) {
        InputStream stream = cacheService.fetch(cacheId, file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                .body(new InputStreamResource(stream));
    }
}
