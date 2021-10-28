/*
 * Copyright 2019 fir.im
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

package com.flowci.core.job.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.core.job.domain.Job.Status;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.githook.domain.Variables;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.HashMap;

/**
 * As job list item, remove the data which is not used in the list view
 *
 * @author yang
 */
@Getter
@Setter
@Document(collection = "job")
public class JobItem extends JobDesc {

    @Getter
    @Setter
    public static class Context extends HashMap<String, String> {

        @JsonIgnore
        private org.bson.Document source;

        public Context(org.bson.Document source) {
            this.source = source;
        }

        public Context() {
            super();
        }

        void putIfNotEmpty(String key) {
            String value = source.getString(key);

            if (Strings.isNullOrEmpty(value)) {
                return;
            }

            this.put(key, value);
        }

    }

    public static class ContextReader implements Converter<org.bson.Document, Context> {

        @Override
        public Context convert(org.bson.Document source) {
            Context context = new Context(source);
            context.putIfNotEmpty(Variables.GIT_EVENT);

            // git push / tag
            context.putIfNotEmpty(Variables.GIT_BRANCH);
            context.putIfNotEmpty(Variables.GIT_COMMIT_ID);
            context.putIfNotEmpty(Variables.GIT_COMMIT_URL);
            context.putIfNotEmpty(Variables.GIT_COMMIT_MESSAGE);

            // git pr
            context.putIfNotEmpty(Variables.PR_TITLE);
            context.putIfNotEmpty(Variables.PR_NUMBER);
            context.putIfNotEmpty(Variables.PR_URL);
            context.putIfNotEmpty(Variables.PR_HEAD_REPO_NAME);
            context.putIfNotEmpty(Variables.PR_HEAD_REPO_BRANCH);
            context.putIfNotEmpty(Variables.PR_BASE_REPO_NAME);
            context.putIfNotEmpty(Variables.PR_BASE_REPO_BRANCH);

            return context;
        }
    }

    private Trigger trigger;

    private Status status;

    private Context context;

    private Date createdAt;

    private Date startAt;

    private Date finishAt;

    private int numOfArtifact;
}
