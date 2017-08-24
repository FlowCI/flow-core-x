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

package com.flow.platform.api.dao;

import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.api.util.JobConvertUtil;
import com.flow.platform.core.dao.AbstractBaseDao;
import java.math.BigInteger;
import java.util.List;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

/**
 * @author yh@firim
 */
@Repository(value = "jobDao")
public class JobDaoImpl extends AbstractBaseDao<BigInteger, Job> implements JobDao {

    @Override
    protected Class<Job> getEntityClass() {
        return Job.class;
    }

    @Override
    protected String getKeyName() {
        return "id";
    }

    private final static String TEMPLATE = ""
        + "job.id, job.number, job.node_name"
        + ", job.node_path, job.session_id"
        + ", node_result.started_at, node_result.finished_at"
        + ", node_result.status, node_result.exit_code"
        + ", node_result.outputs"
        + ", node_result.duration"
        + ", job.cmd_id, job.created_at, job.updated_at ";


    @Override
    public List<Job> list() {
        return execute((Session session) -> {
            String select = String
                .format(
                    "select %s"
                        + "from job left join node_result "
                        + "on "
                        + " job.node_path=node_result.node_path "
                        + " and job.id=node_result.job_id ",
                    TEMPLATE);

            List<Object[]> objects = (List<Object[]>) session.createNativeQuery(select)
                .list();

            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public List<Job> list(List<String> sessionIds, NodeStatus nodeStatus) {
        return execute((Session session) -> {
            StringBuilder stringBuilder = new StringBuilder("");
            for (String sessionId : sessionIds) {
                stringBuilder.append(" '" + sessionId + "' ");
            }

            String string = stringBuilder.toString();
            string = string.substring(0, string.length() - 1);

            String select = String
                .format(
                    "select %s"
                        + "from job left join node_result "
                        + "on "
                        + " job.node_path=node_result.node_path "
                        + " and job.id=node_result.job_id "
                        + "where job.session_id in ( %s ) "
                        + " and node_result.status='%s'",
                    TEMPLATE, string, nodeStatus.getName());

            List<Object[]> objects = (List<Object[]>) session.createNativeQuery(select)
                .list();

            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public Job get(BigInteger key) {
        return execute((Session session) -> {
            String select = String
                .format(
                    "select %s"
                        + "from job left join node_result "
                        + "on "
                        + " job.node_path=node_result.node_path "
                        + " and job.id=node_result.job_id "
                        + "where id=%s",
                    TEMPLATE, key);

            Object[] objects = (Object[]) session.createNativeQuery(select)
                .uniqueResult();

            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public List<Job> listLatest(List<String> nodeNames) {
        return execute((Session session) -> {
            String string = String.join("','", nodeNames);
            string = "'" + string + "'";

            String select = String
                .format(
                    "select %s"
                        + "from job left join node_result "
                        + "on "
                        + " job.node_path=node_result.node_path "
                        + " and job.id=node_result.job_id "
                        + "where id in (select max(id) from Job where nodeName in ( %s ) group by nodePath)",
                    TEMPLATE, string);

            List<Job> jobs = (List<Job>) session.createQuery(select)
                .list();
            return jobs;
        });
    }


    @Override
    public List<Job> list(String nodePath) {
        return execute((Session session) -> {
            String select = String
                .format(
                    "select %s"
                        + "from job left join node_result "
                        + "on "
                        + " job.node_path=node_result.node_path "
                        + " and job.id=node_result.job_id "
                        + " where job.node_path='%s'",
                    TEMPLATE, nodePath);

            List<Object[]> objects = (List<Object[]>) session.createNativeQuery(select)
                .list();

            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public Job get(String flowName, Integer number) {
        return execute((Session session) -> {
            String select = String
                .format(
                    "select %s"
                        + "from job left join node_result "
                        + "on "
                        + " job.node_path=node_result.node_path "
                        + " and job.id=node_result.job_id "
                        + "where job.node_name='%s' and job.number=%s",
                    TEMPLATE, flowName, number);

            Object[] objects = (Object[]) session.createNativeQuery(select)
                .uniqueResult();

            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public Integer maxBuildNumber(String flowName) {
        return execute((Session session) -> {
            String select = String.format("select max(number) from Job where node_name='%s'", flowName);
            Integer integer = (Integer) session.createQuery(select).uniqueResult();
            if (integer == null) {
                integer = 0;
            }
            return integer;
        });
    }
}
