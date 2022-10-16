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

package com.flowci.core.job.service;

import com.flowci.core.job.dao.JobCacheDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobCache;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.store.FileManager;
import com.flowci.store.Pathable;
import com.flowci.store.StringPath;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;

@Log4j2
@Service
public class CacheServiceImpl implements CacheService {

    private final static Pathable CacheRoot = new StringPath("_cache_");

    @Autowired
    private JobCacheDao jobCacheDao;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private JobService jobService;

    @Override
    public JobCache put(String jobId, String key, String os, MultipartFile[] files) {
        Job job = jobService.get(jobId);

        JobCache entity = new JobCache();
        Optional<JobCache> optional = jobCacheDao.findByFlowIdAndKey(job.getFlowId(), key);
        if (optional.isPresent()) {
            entity = optional.get();
        }

        entity.setJobId(job.getId());
        entity.setFlowId(job.getFlowId());
        entity.setKey(key);
        entity.setOs(os);
        entity.setFiles(new ArrayList<>(files.length));

        for (MultipartFile file : files) {
            try {
                Pathable[] cachePath = getCachePath(job.getFlowId(), key);
                fileManager.save(file.getOriginalFilename(), file.getInputStream(), cachePath);
                entity.getFiles().add(file.getOriginalFilename());
            } catch (IOException e) {
                log.warn("failed to save file {} for cache {}", file.getName(), key);
            }
        }

        return jobCacheDao.save(entity);
    }

    @Override
    public JobCache get(String jobId, String key) {
        Job job = jobService.get(jobId);

        Optional<JobCache> optional = jobCacheDao.findByFlowIdAndKey(job.getFlowId(), key);
        if (!optional.isPresent()) {
            throw new NotFoundException("Cache not found");
        }

        return optional.get();
    }

    @Override
    public InputStream fetch(String cacheId, String file) {
        Optional<JobCache> optional = jobCacheDao.findById(cacheId);
        if (!optional.isPresent()) {
            throw new NotFoundException("Cache not found");
        }

        JobCache cache = optional.get();
        if (!cache.getFiles().contains(file)) {
            throw new NotFoundException("file not found");
        }

        Pathable[] cachePath = getCachePath(cache.getFlowId(), cache.getKey());
        if (!fileManager.exist(file, cachePath)) {
            throw new NotFoundException("file not found in file store");
        }

        try {
            return fileManager.read(file, cachePath);
        } catch (IOException e) {
            throw new StatusException("unable to read cache file");
        }
    }

    private Pathable[] getCachePath(String flowId, String key) {
        return new Pathable[]{
                new StringPath(flowId),
                CacheRoot,
                new StringPath(key)
        };
    }
}
