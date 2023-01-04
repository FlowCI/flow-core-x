/*
 * Copyright 2023 flow.ci
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

package com.flowci.core.upgrade;

import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.flow.domain.FlowYml;
import com.flowci.core.flow.domain.SimpleYml;
import com.flowci.core.job.domain.JobYml;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Upgrade Yml and Job data from version 1.22.xx to 1.23.xx
 */
@Log4j2
@Component
public class Ver122to123 implements CommandLineRunner {

    @Getter
    @Setter
    public static class FlowYmlDeprecatedOnVer122 extends Mongoable {
        private String flowId;

        private String name;

        private String rawInB64;

        public FlowYml toNewEntity() {
            var newYmlItem = new FlowYml();
            newYmlItem.setId(id);
            newYmlItem.setFlowId(flowId);
            newYmlItem.setList(List.of(new SimpleYml(FlowYml.DEFAULT_NAME, rawInB64)));
            return newYmlItem;
        }
    }

    @Getter
    @Setter
    public static class JobYmlDeprecatedOnVer122 extends Mongoable {

        private String raw;

        public JobYml toNewEntity() {
            var newEntity = new JobYml();
            newEntity.setId(this.id);

            var body = new JobYml.Body(FlowYml.DEFAULT_NAME, StringHelper.toBase64(raw));
            newEntity.setList(List.of(body));
            return newEntity;
        }
    }

    private final MongoTemplate mongoTemplate;

    public Ver122to123(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
       log.info("Run upgrade code from version 1.22.xx to 1.23.xx");
        restructureFlowYmlData();
        restructureJobYmlData();
    }

    private void restructureFlowYmlData() {
        var query = new Query().addCriteria(Criteria.where("name").exists(true));
        var list = mongoTemplate.find(query, FlowYmlDeprecatedOnVer122.class, "flow_yml");

        var iter = list.iterator();
        while (iter.hasNext()) {
            var oldYmlItem = iter.next();
            this.mongoTemplate.save(oldYmlItem.toNewEntity());
            log.info("-- Yml structure updated on flow id: {}", oldYmlItem.getId());
            iter.remove();
        }
    }

    private void restructureJobYmlData() {
        var query = new Query().addCriteria(Criteria.where("raw").exists(true));
        var list = mongoTemplate.find(query, JobYmlDeprecatedOnVer122.class, "job_yml");

        var iter = list.iterator();
        while (iter.hasNext()) {
            var oldYmlItem = iter.next();
            this.mongoTemplate.save(oldYmlItem.toNewEntity());
            log.info("-- Yml structure updated on job id: {}", oldYmlItem.getId());
            iter.remove();
        }
    }
}
