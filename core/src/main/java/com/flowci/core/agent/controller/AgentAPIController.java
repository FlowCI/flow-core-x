package com.flowci.core.agent.controller;

import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.job.domain.JobCache;
import com.flowci.core.job.service.CacheService;
import com.flowci.core.job.service.LoggingService;
import com.flowci.exception.ArgumentException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Functions require agent token header
 */
@Log4j2
@RestController
@RequestMapping("/agents/api")
public class AgentAPIController {

    @Autowired
    private AgentService agentService;

    @Autowired
    private LoggingService loggingService;

    @Autowired
    private CacheService cacheService;

    @PostMapping("/profile")
    public void profile(@RequestHeader(AgentAuth.HeaderAgentToken) String token,
                        @RequestBody Agent.Resource resource) {
        agentService.update(token, resource);
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
