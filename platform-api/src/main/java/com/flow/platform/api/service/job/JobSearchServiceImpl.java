/*
 * Copyright 2017 flow.ci
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

package com.flow.platform.api.service.job;

import com.flow.platform.api.domain.SearchCondition;
import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.api.domain.job.Job;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author gyfirim
 */

@Service(value = "searchService")
public class JobSearchServiceImpl implements JobSearchService {

    private static List<Condition> conditions = new ArrayList<>(3);

    static {
        conditions.add(new KeywordCondition());
        conditions.add(new BranchCondition());
        conditions.add(new GitCondition());
        conditions.add(new CreatorCondition());
    }

    @Autowired
    private JobService jobService;

    @Override
    public List<Job> search(SearchCondition searchCondition, List<String> paths) {
        List<Job> jobs = jobService.list(paths, false);
        return match(searchCondition, jobs);
    }

    private List<Job> match(SearchCondition searchCondition, List<Job> jobs) {
        for (Condition condition : conditions) {
            jobs = condition.match(searchCondition, jobs);
        }
        return jobs;
    }

    private interface Condition {

        List<Job> match(SearchCondition searchCondition, List<Job> jobs);
    }


    /**
     * keyword search match  number or  branch
     */
    private static class KeywordCondition implements Condition {

        @Override
        public List<Job> match(SearchCondition searchCondition, List<Job> jobs) {
            String words = searchCondition.getKeyword();

            if (Strings.isNullOrEmpty(words)) {
                return jobs;
            }

            List<Job> copyJobs = new LinkedList<>();

            for (Job job : jobs) {
                if (job.getNumber().toString().equals(words)) { // compare job number
                    copyJobs.add(job);
                } else if (words.equals(
                    job.getRootResult().getOutputs().get(GitEnvs.FLOW_GIT_BRANCH.toString()))) { //compare branch
                    copyJobs.add(job);
                }
            }
            return copyJobs;
        }
    }

    /**
     * branch search
     */
    private static class BranchCondition implements Condition {

        @Override
        public List<Job> match(SearchCondition searchCondition, List<Job> jobs) {
            String branch = searchCondition.getBranch();
            if (Strings.isNullOrEmpty(branch)) {
                return jobs;
            }

            List<Job> copyJobs = new LinkedList<>();
            for (Job job : jobs) {
                if (branch.equals(job.getRootResult().getOutputs().get(GitEnvs.FLOW_GIT_BRANCH.toString()))) {
                    copyJobs.add(job);
                }
            }

            return copyJobs;
        }
    }

    /**
     * git type search
     */
    private static class GitCondition implements Condition {

        @Override
        public List<Job> match(SearchCondition searchCondition, List<Job> jobs) {
            if (searchCondition.getCategory() == null) {
                return jobs;
            }

            List<Job> copyJobs = new LinkedList<>();
            for (Job job : jobs) {
                if (searchCondition.getCategory().equals(job.getCategory().toString())) {
                    copyJobs.add(job);
                }
            }

            return copyJobs;
        }
    }

    /**
     * creator search
     */
    private static class CreatorCondition implements Condition {

        @Override
        public List<Job> match(SearchCondition searchCondition, List<Job> jobs) {
            if (searchCondition.getCreator() == null) {
                return jobs;
            }

            List<Job> copyJobs = new LinkedList<>();
            for (Job job : jobs) {
                if (searchCondition.getCreator().equals(job.getCreatedBy())) {
                    copyJobs.add(job);
                }
            }

            return copyJobs;
        }
    }
}
