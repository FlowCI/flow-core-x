package com.flowci.core.user.config;

import com.flowci.core.common.helper.CacheHelper;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserConfig {

    @Bean("userCacheManager")
    public CacheManager userCacheManager() {
        return CacheHelper.createCacheManager(1800, 100);
    }
}
