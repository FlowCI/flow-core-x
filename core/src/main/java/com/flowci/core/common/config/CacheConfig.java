package com.flowci.core.common.config;

import com.flowci.core.common.helper.CacheHelper;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean("defaultCacheManager")
    public CacheManager defaultCacheManager() {
        return CacheHelper.createCacheManager(1800, 100);
    }
}
