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
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.HashMap;

import static com.flowci.core.common.domain.Variables.Git.*;

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
            context.putIfNotEmpty(EVENT);

            // git push / tag
            context.putIfNotEmpty(PUSH_BRANCH);
            context.putIfNotEmpty(PUSH_MESSAGE);
            context.putIfNotEmpty(PUSH_AUTHOR);
            context.putIfNotEmpty(PUSH_COMMIT_LIST);
            context.putIfNotEmpty(PUSH_COMMIT_TOTAL);

            // git pr
            context.putIfNotEmpty(PR_TITLE);
            context.putIfNotEmpty(PR_NUMBER);
            context.putIfNotEmpty(PR_URL);
            context.putIfNotEmpty(PR_HEAD_REPO_NAME);
            context.putIfNotEmpty(PR_HEAD_REPO_BRANCH);
            context.putIfNotEmpty(PR_BASE_REPO_NAME);
            context.putIfNotEmpty(PR_BASE_REPO_BRANCH);

            // git patchset
            context.putIfNotEmpty(PATCHSET_SUBJECT);
            context.putIfNotEmpty(PATCHSET_BRANCH);
            context.putIfNotEmpty(PATCHSET_CHANGE_ID);
            context.putIfNotEmpty(PATCHSET_PATCH_NUM);
            context.putIfNotEmpty(PATCHSET_PATCH_URL);
            context.putIfNotEmpty(PATCHSET_AUTHOR);
            context.putIfNotEmpty(PATCHSET_REF);

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
