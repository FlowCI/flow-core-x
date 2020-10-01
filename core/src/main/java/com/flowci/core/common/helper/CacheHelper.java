/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.common.helper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.concurrent.TimeUnit;

public class CacheHelper {

    public static <K, V> Cache<K, V> createLocalCache(int maxSize, int expireInSeconds) {
        return Caffeine.newBuilder()
                .initialCapacity(maxSize)
                .maximumSize(maxSize)
                .expireAfterWrite(expireInSeconds, TimeUnit.SECONDS)
                .build();
    }

    public static <K, V> Cache<K, V> createLocalCache(int maxSize, int expireInSeconds, RemovalListener<K, V> listener) {
        return Caffeine.newBuilder()
                .initialCapacity(maxSize)
                .maximumSize(maxSize)
                .removalListener(listener)
                .expireAfterWrite(expireInSeconds, TimeUnit.SECONDS)
                .build();
    }

    public static CacheManager createCacheManager(int expireInSeconds, int maxSize) {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .initialCapacity(maxSize)
                .maximumSize(maxSize)
                .expireAfterWrite(expireInSeconds, TimeUnit.SECONDS);

        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeine);
        return caffeineCacheManager;
    }

    private CacheHelper() {

    }
}
