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

package com.flow.platform.api.dao.job;

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.dao.util.JobConvertUtil;
import com.flow.platform.core.dao.AbstractBaseDao;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;

/**
 * @author yh@firim
 */
@Repository(value = "jobDao")
public class JobDaoImpl extends AbstractBaseDao<BigInteger, Job> implements JobDao {

    private final static String JOB_QUERY = "select * from job as job left join node_result as nr "
        + "on job.node_path=nr.node_path and job.id=nr.job_id";

    @Override
    protected Class<Job> getEntityClass() {
        return Job.class;
    }

    @Override
    protected String getKeyName() {
        return "id";
    }

    @Override
    public List<Job> list() {
        return execute((Session session) -> {
            NativeQuery nativeQuery = session.createNativeQuery(JOB_QUERY).setResultSetMapping("MappingJobResult");
            List<Object[]> objects = nativeQuery.list();
            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public List<Job> list(List<String> sessionIds, NodeStatus nodeStatus) {
        return execute((Session session) -> {
            NativeQuery nativeQuery = session.createNativeQuery(
                JOB_QUERY + " where job.session_id in (:sessionIds) and nr.node_status=:status")
                .setParameter("sessionIds", sessionIds)
                .setParameter("status", nodeStatus.getName())
                .setResultSetMapping("MappingJobResult");

            List<Object[]> objects = nativeQuery.list();
            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public Job get(BigInteger key) {
        return execute((Session session) -> {
            NativeQuery nativeQuery = session.createNativeQuery(JOB_QUERY + " where job.id=:id")
                .setParameter("id", key)
                .setResultSetMapping("MappingJobResult");

            Object[] objects = (Object[]) nativeQuery.uniqueResult();
            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public List<Job> latestByPath(List<String> paths) {
        return execute((Session session) -> {
            final StringBuilder query = new StringBuilder(JOB_QUERY);
            if (hasCollection(paths)) {
                query.append(" where id in (select max(id) from job where node_path in (:paths) group by node_path)");
            } else {
                query.append(" where id in (select max(id) from job group by node_path)");
            }

            NativeQuery nativeQuery = session.createNativeQuery(query.toString())
                .setResultSetMapping("MappingJobResult");
            if (hasCollection(paths)) {
                nativeQuery.setParameterList("paths", paths);
            }

            List<Object[]> objects = nativeQuery.list();
            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public List<Job> listByPath(final List<String> paths) {
        return execute((Session session) -> {
            final StringBuilder query = new StringBuilder(JOB_QUERY);
            if (hasCollection(paths)) {
                query.append(" where job.node_path in (:paths) order by job.created_at desc ");
            }

            NativeQuery nativeQuery = session.createNativeQuery(query.toString())
                .setResultSetMapping("MappingJobResult");
            if (hasCollection(paths)) {
                nativeQuery.setParameterList("paths", paths);
            }

            List<Object[]> objects = nativeQuery.list();
            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public Job get(String path, Integer number) {
        return execute((Session session) -> {
            NativeQuery nativeQuery = session.createNativeQuery(
                JOB_QUERY + " where job.node_path=:name and job.build_number=:build_number")
                .setParameter("name", path)
                .setParameter("build_number", number)
                .setResultSetMapping("MappingJobResult");
            Object[] objects = (Object[]) nativeQuery.uniqueResult();
            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public Integer maxBuildNumber(String path) {
        return execute((Session session) -> {
            String select = "select max(build_number) from job where node_path=:node_path";
            Integer integer = (Integer) session
                .createNativeQuery(select)
                .setParameter("node_path", path)
                .uniqueResult();

            if (integer == null) {
                integer = 0;
            }

            return integer;
        });
    }

    @Override
    public List<BigInteger> findJobIdsByPath(String path){
        return execute(session -> session
            .createQuery("select id from Job where nodePath = ?", BigInteger.class)
            .setParameter(0, path)
            .list());
    }


    @Override
    public int deleteJob(String path){
        return execute(session -> session
            .createQuery("delete from Job where nodePath = ?")
            .setParameter(0, path)
            .executeUpdate());
    }

    private static boolean hasCollection(final Collection<String> data) {
        return data != null && data.size() > 0;
    }
}
