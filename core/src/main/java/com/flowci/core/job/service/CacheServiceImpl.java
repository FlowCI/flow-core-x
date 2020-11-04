package com.flowci.core.job.service;

import com.flowci.core.job.domain.Job;
import com.flowci.store.FileManager;
import com.flowci.store.Pathable;
import com.flowci.store.StringPath;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Log4j2
@Service
public class CacheServiceImpl implements CacheService {

    private final static Pathable CacheRoot = new StringPath("_cache_");

    @Autowired
    private FileManager fileManager;

    @Override
    public void put(Job job, String key, MultipartFile[] files) {
        for (MultipartFile file : files) {
            try {
                Pathable[] cachePath = getCachePath(job, key);
                fileManager.save(file.getOriginalFilename(), file.getInputStream(), cachePath);
            } catch (IOException e) {
                log.warn("failed to save file {} for cache {}", file.getName(), key);
            }
        }
    }

    private Pathable[] getCachePath(Job job, String key) {
        return new Pathable[]{
                new StringPath(job.getFlowId()),
                CacheRoot,
                new StringPath(key)
        };
    }
}
