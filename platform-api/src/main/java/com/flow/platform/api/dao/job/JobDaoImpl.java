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

import static com.flow.platform.core.dao.PageUtil.buildPage;

import com.flow.platform.api.dao.util.JobConvertUtil;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.core.dao.AbstractBaseDao;
import com.flow.platform.core.domain.Page;
import com.flow.platform.core.domain.Pageable;
import com.flow.platform.util.CollectionUtil;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.List;
import javax.persistence.TypedQuery;
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

    private final static String JOB_COUNT_QUERY = "select count(*) from job as job left join node_result as nr "
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
                JOB_QUERY + " WHERE job.session_id IN (:sessionIds) AND nr.node_status=:status")
                .setParameter("sessionIds", sessionIds)
                .setParameter("status", nodeStatus.getName())
                .setResultSetMapping("MappingJobResult");

            List<Object[]> objects = nativeQuery.list();
            return JobConvertUtil.convert(objects);
        });
    }


    @Override
    public Page<Job> list(List<String> sessionIds, NodeStatus nodeStatus, Pageable pageable) {
        return execute((Session session) -> {
            NativeQuery nativeQuery = session.createNativeQuery(
                JOB_QUERY + " WHERE job.session_id IN (:sessionIds) AND nr.node_status=:status")
                .setParameter("sessionIds", sessionIds)
                .setParameter("status", nodeStatus.getName())
                .setResultSetMapping("MappingJobResult");

            nativeQuery.setFirstResult(pageable.getOffset());
            nativeQuery.setMaxResults(pageable.getPageSize());
            List<Object[]> objects = nativeQuery.list();
            List<Job> jobs = JobConvertUtil.convert(objects);
            return new Page<>(jobs, pageable.getPageSize(), pageable.getPageNumber(), new TotalSupplier() {
                @Override
                public long get() {
                    NativeQuery nativeQuery = session.createNativeQuery(
                        JOB_COUNT_QUERY + " WHERE job.session_id IN (:sessionIds) AND nr.node_status=:status")
                        .setParameter("sessionIds", sessionIds)
                        .setParameter("status", nodeStatus.getName());
                    return Long.valueOf(nativeQuery.uniqueResult().toString());
                }
            });
        });
    }

    @Override
    public Job get(BigInteger key) {
        return execute((Session session) -> {
            NativeQuery nativeQuery = session.createNativeQuery(JOB_QUERY + " WHERE job.id=:id")
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

            if (!CollectionUtil.isNullOrEmpty(paths)) {
                query.append(" where id in (select max(id) from job where node_path in (:paths) group by node_path)");
            } else {
                query.append(" where id in (select max(id) from job group by node_path)");
            }

            NativeQuery nativeQuery = session
                .createNativeQuery(query.toString())
                .setResultSetMapping("MappingJobResult");

            if (!CollectionUtil.isNullOrEmpty(paths)) {
                nativeQuery.setParameterList("paths", paths);
            }

            List<Object[]> objects = nativeQuery.list();
            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public Page<Job> latestByPath(List<String> paths, Pageable pageable) {
        return execute((Session session) -> {
            final StringBuilder query = new StringBuilder(JOB_QUERY);

            if (!CollectionUtil.isNullOrEmpty(paths)) {
                query.append(" where id in (select max(id) from job where node_path in (:paths) group by node_path)");
            } else {
                query.append(" where id in (select max(id) from job group by node_path)");
            }

            NativeQuery nativeQuery = session
                .createNativeQuery(query.toString())
                .setResultSetMapping("MappingJobResult");

            if (!CollectionUtil.isNullOrEmpty(paths)) {
                nativeQuery.setParameterList("paths", paths);
            }

            nativeQuery.setFirstResult(pageable.getOffset());
            nativeQuery.setMaxResults(pageable.getPageSize());

            List<Object[]> objects = nativeQuery.list();
            List<Job> jobs = JobConvertUtil.convert(objects);

            return new Page<>(jobs, pageable.getPageSize(), pageable.getPageNumber(), new TotalSupplier() {
                @Override
                public long get() {
                    StringBuilder query = new StringBuilder(JOB_COUNT_QUERY);

                    if (!CollectionUtil.isNullOrEmpty(paths)) {
                        query.append(
                            " where id in (select max(id) from job where node_path in (:paths) group by node_path)");
                    } else {
                        query.append(" where id in (select max(id) from job group by node_path)");
                    }

                    NativeQuery nativeQuery = session.createNativeQuery(query.toString());

                    if (!CollectionUtil.isNullOrEmpty(paths)) {
                        nativeQuery.setParameterList("paths", paths);
                    }

                    return Long.valueOf(nativeQuery.uniqueResult().toString());
                }
            });
        });
    }

    @Override
    public List<Job> listByPath(final List<String> paths) {
        return execute((Session session) -> {
            final StringBuilder query = new StringBuilder(JOB_QUERY);

            if (!CollectionUtil.isNullOrEmpty(paths)) {
                query.append(" where job.node_path in (:paths) order by job.created_at desc ");
            }

            NativeQuery nativeQuery = session.createNativeQuery(query.toString())
                .setResultSetMapping("MappingJobResult");

            if (!CollectionUtil.isNullOrEmpty(paths)) {
                nativeQuery.setParameterList("paths", paths);
            }

            List<Object[]> objects = nativeQuery.list();
            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public Page<Job> listByPath(List<String> paths, Pageable pageable) {
        return execute((Session session) -> {
            final StringBuilder query = new StringBuilder(JOB_QUERY);

            if (!CollectionUtil.isNullOrEmpty(paths)) {
                query.append(" where job.node_path in (:paths) order by job.created_at desc ");
            }

            NativeQuery nativeQuery = session.createNativeQuery(query.toString())
                .setResultSetMapping("MappingJobResult");

            if (!CollectionUtil.isNullOrEmpty(paths)) {
                nativeQuery.setParameterList("paths", paths);
            }

            nativeQuery.setFirstResult(pageable.getOffset());
            nativeQuery.setMaxResults(pageable.getPageSize());

            List<Object[]> objects = nativeQuery.list();
            List<Job> jobs = JobConvertUtil.convert(objects);

            return new Page<>(jobs, pageable.getPageSize(), pageable.getPageNumber(), new TotalSupplier() {
                @Override
                public long get() {
                    final StringBuilder query = new StringBuilder(JOB_COUNT_QUERY);

                    if (!CollectionUtil.isNullOrEmpty(paths)) {
                        query.append(" where job.node_path in (:paths) order by job.created_at desc ");
                    }

                    NativeQuery nativeQuery = session.createNativeQuery(query.toString());

                    if (!CollectionUtil.isNullOrEmpty(paths)) {
                        nativeQuery.setParameterList("paths", paths);
                    }

                    return Long.valueOf(nativeQuery.uniqueResult().toString());
                }
            });
        });
    }

    @Override
    public List<Job> listByStatus(EnumSet<JobStatus> status) {
        return execute(session -> session.createQuery("from Job where status in :status", Job.class)
            .setParameterList("status", status)
            .list());
    }

    @Override
    public Page<Job> listByStatus(EnumSet<JobStatus> status, Pageable pageable) {
        return execute(session -> {
            TypedQuery query = session.createQuery("from Job where status in :status", Job.class)
                .setParameterList("status", status);
            return buildPage(query, pageable, new TotalSupplier() {
                @Override
                public long get() {
                    return (long) session.createQuery("select count(*) from Job where status in :status")
                        .setParameterList("status", status)
                        .uniqueResult();
                }
            });
        });
    }

    @Override
    public Job get(String path, Long number) {
        return execute((Session session) -> {
            NativeQuery nativeQuery = session.createNativeQuery(
                JOB_QUERY + " WHERE job.node_path=:name AND job.build_number=:build_number")
                .setParameter("name", path)
                .setParameter("build_number", number)
                .setResultSetMapping("MappingJobResult");
            Object[] objects = (Object[]) nativeQuery.uniqueResult();
            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public Job get(String sessionId) {
        return execute(session -> session.createQuery("from Job where sessionId = :sessionId", Job.class)
            .setParameter("sessionId", sessionId)
            .uniqueResult());
    }

    @Override
    public List<BigInteger> findJobIdsByPath(String path) {
        return execute(session -> session
            .createQuery("select id from Job where nodePath = :node_path", BigInteger.class)
            .setParameter("node_path", path)
            .list());
    }

    @Override
    public Page<BigInteger> findJobIdsByPath(String path, Pageable pageable) {
        return execute(session -> {
            TypedQuery query = session
                .createQuery("select id from Job where nodePath = :node_path", BigInteger.class)
                .setParameter("node_path", path);

            return buildPage(query, pageable, new TotalSupplier() {
                @Override
                public long get() {
                    return (long) session
                        .createQuery("select count(id) from Job where nodePath = :node_path")
                        .setParameter("node_path", path)
                        .uniqueResult();
                }
            });
        });
    }

    @Override
    public int deleteJob(String path) {
        return execute(session -> session
            .createQuery("delete from Job where nodePath = ?")
            .setParameter(0, path)
            .executeUpdate());
    }

    @Override
    public Long numOfJob(String path) {
        return execute(session ->
            session.createQuery("select count(id) from Job where nodePath = :node_path", Long.class)
                .setParameter("node_path", path)
                .uniqueResult()
        );
    }
}
