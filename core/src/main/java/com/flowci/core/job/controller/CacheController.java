package com.flowci.core.job.controller;

import com.flowci.core.job.domain.Job;
import com.flowci.core.job.service.CacheService;
import com.flowci.exception.ArgumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/jobs")
public class CacheController extends BaseController {

    @Autowired
    private CacheService cacheService;

    @PostMapping("/{flow}/{buildNumber}/cache/{key}")
    public void put(@PathVariable String flow,
                    @PathVariable String buildNumber,
                    @PathVariable String key,
                    @RequestParam("files") MultipartFile[] files) {
        if (files.length == 0) {
            throw new ArgumentException("the cached files are empty");
        }

        Job job = getJob(flow, buildNumber);
        cacheService.put(job, key, files);
    }
}
