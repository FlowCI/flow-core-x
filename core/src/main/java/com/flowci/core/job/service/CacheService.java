package com.flowci.core.job.service;

import com.flowci.core.job.domain.Job;
import org.springframework.web.multipart.MultipartFile;

/**
 * Job Cache to save cached file/dir into file store
 */
public interface CacheService {

    void put(Job job, String key, MultipartFile[] files);

}
