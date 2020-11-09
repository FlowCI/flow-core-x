package com.flowci.core.job.service;

import com.flowci.core.job.domain.JobCache;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Job Cache to save cached file/dir into file store
 */
public interface CacheService {

    JobCache put(String jobId, String key, String os, MultipartFile[] files);

    JobCache get(String jobId, String key);

    InputStream fetch(String cacheId, String file);
}
