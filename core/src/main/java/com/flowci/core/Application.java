/*
 * Copyright 2018 flow.ci
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

package com.flowci.core;

import com.flowci.common.helper.StringHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

/**
 * @author yang
 */
@Slf4j
@SpringBootApplication
public class Application {

    @Autowired
    private ResourceLoader resourceLoader;

    @EventListener(ContextRefreshedEvent.class)
    public void printBanner() throws IOException {
        Resource r = resourceLoader.getResource("classpath:welcome.txt");
        log.info(StringHelper.toString(r.getInputStream()));
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
