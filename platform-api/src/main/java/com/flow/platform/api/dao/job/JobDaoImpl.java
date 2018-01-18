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

import com.flow.platform.api.dao.util.JobConvertUtil;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.core.dao.AbstractBaseDao;
import com.flow.platform.core.dao.QueryHelper;
import com.flow.platform.core.dao.QueryHelper.Builder;
import com.flow.platform.core.domain.Page;
import com.flow.platform.core.domain.Pageable;
import com.flow.platform.util.CollectionUtil;
import com.flow.platform.util.ObjectUtil;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;

/**
 * @author yh@firim
 */
@Repository(value = "jobDao")
public class JobDaoImpl extends AbstractBaseDao<BigInteger, Job> implements JobDao {

    private final static String JOB_QUERY_SELECT = "*";

    private final static String JOB_QUERY_FROM = "job as job left join node_result as nr on job.node_path=nr.node_path and job.id=nr.job_id";

    private final static Builder QUERY_BUILDER = QueryHelper.Builder()
                                .select(JOB_QUERY_SELECT)
                                .from(JOB_QUERY_FROM);

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
            NativeQuery nativeQuery = ObjectUtil.deepCopy(QUERY_BUILDER).createNativeQuery(session);
            nativeQuery.setResultSetMapping("MappingJobResult");
            List<Object[]> objects = nativeQuery.list();
            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public List<Job> list(List<String> sessionIds, NodeStatus nodeStatus) {
        return execute((Session session) -> {
            String where = "job.session_id IN (:sessionIds) AND nr.node_status=:status";
            Builder builder = ObjectUtil.deepCopy(QUERY_BUILDER);
            builder.where(where)
                .parameter("sessionIds", sessionIds)
                .parameter("status", nodeStatus.getName());
            NativeQuery nativeQuery = builder.createNativeQuery(session).setResultSetMapping("MappingJobResult");
            List<Object[]> objects = nativeQuery.list();
            return JobConvertUtil.convert(objects);
        });
    }


    @Override
    public Page<Job> list(List<String> sessionIds, NodeStatus nodeStatus, Pageable pageable) {
        return execute((Session session) -> {

            String countSelect = "count(*)";
            String where = "job.session_id IN (:sessionIds) AND nr.node_status=:status";

            Builder builder = ObjectUtil.deepCopy(QUERY_BUILDER);

            builder.where(where)
                .parameter("sessionIds", sessionIds)
                .parameter("status", nodeStatus.getName());

            NativeQuery nativeQuery = builder.createNativeQuery(session);
            nativeQuery.setFirstResult(pageable.getOffset());
            nativeQuery.setMaxResults(pageable.getPageSize());
            nativeQuery.setResultSetMapping("MappingJobResult");
            List<Object[]> objects = nativeQuery.list();
            List<Job> jobs = JobConvertUtil.convert(objects);

            NativeQuery countNativeQuery = builder.select(countSelect).createNativeQuery(session);
            long totalSize = Long.valueOf(countNativeQuery.uniqueResult().toString());

            return new Page<>(jobs, pageable.getPageSize(), pageable.getPageNumber(), totalSize);
        });
    }

    @Override
    public Job get(BigInteger key) {
        return execute((Session session) -> {
            String where = "job.id=:id";
            Builder builder = ObjectUtil.deepCopy(QUERY_BUILDER)
                .where(where)
                .parameter("id", key);
            NativeQuery nativeQuery = builder.createNativeQuery(session).setResultSetMapping("MappingJobResult");

            Object[] objects = (Object[]) nativeQuery.uniqueResult();
            return JobConvertUtil.convert(objects);
        });
    }


    @Override
    public List<Job> latestByPath(List<String> paths) {
        return execute((Session session) -> {

            String where = "id in (select max(id) from job group by node_path)";
            if (!CollectionUtil.isNullOrEmpty(paths)) {
                where = "id in (select max(id) from job where node_path in (:paths) group by node_path)";
            }

            Builder builder = ObjectUtil.deepCopy(QUERY_BUILDER);
            builder.where(where);

            if (!CollectionUtil.isNullOrEmpty(paths)) {
                builder.parameter("paths", paths);
            }

            NativeQuery nativeQuery = builder.createNativeQuery(session);
            nativeQuery.setResultSetMapping("MappingJobResult");

            List<Object[]> objects = nativeQuery.list();
            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public Page<Job> latestByPath(List<String> paths, Pageable pageable) {
        return execute((Session session) -> {

            String countSelect = "count(*)";
            String where = "id in (select max(id) from job group by node_path)";
            if (!CollectionUtil.isNullOrEmpty(paths)) {
                where = "id in (select max(id) from job where node_path in (:paths) group by node_path)";
            }

            Builder builder = ObjectUtil.deepCopy(QUERY_BUILDER);
            builder.where(where);

            if (!CollectionUtil.isNullOrEmpty(paths)) {
                builder.parameter("paths", paths);
            }

            NativeQuery nativeQuery = builder.createNativeQuery(session);
            nativeQuery.setFirstResult(pageable.getOffset());
            nativeQuery.setMaxResults(pageable.getPageSize());
            nativeQuery.setResultSetMapping("MappingJobResult");
            List<Object[]> objects = nativeQuery.list();
            List<Job> jobs = JobConvertUtil.convert(objects);

            NativeQuery countNativeQuery = builder.select(countSelect).createNativeQuery(session);
            long totalSize = Long.valueOf(countNativeQuery.uniqueResult().toString());

            return new Page<>(jobs, pageable.getPageSize(), pageable.getPageNumber(), totalSize);
        });
    }

    @Override
    public List<Job> listByPath(final List<String> paths) {
        return execute((Session session) -> {

            String where = "job.node_path in (:paths) order by job.created_at desc ";

            Builder builder = ObjectUtil.deepCopy(QUERY_BUILDER);

            if (!CollectionUtil.isNullOrEmpty(paths)) {
                builder.where(where).parameter("paths", paths);
            }

            NativeQuery nativeQuery = builder.createNativeQuery(session)
                .setResultSetMapping("MappingJobResult");

            List<Object[]> objects = nativeQuery.list();
            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public Page<Job> listByPath(List<String> paths, Pageable pageable) {
        return execute((Session session) -> {

            String countSelect = "count(*)";
            String where = "";
            if (!CollectionUtil.isNullOrEmpty(paths)){
                where = "job.node_path in (:paths) order by job.created_at desc ";
            }

            Builder builder = ObjectUtil.deepCopy(QUERY_BUILDER);
            builder.where(where);

            if (!CollectionUtil.isNullOrEmpty(paths)){
                builder.parameter("paths",paths);
            }

            NativeQuery nativeQuery = builder.createNativeQuery(session);
            nativeQuery.setResultSetMapping("MappingJobResult");
            nativeQuery.setFirstResult(pageable.getOffset());
            nativeQuery.setMaxResults(pageable.getPageSize());
            List<Object[]> objects = nativeQuery.list();
            List<Job> jobs = JobConvertUtil.convert(objects);

            NativeQuery countNativeQuery = builder.select(countSelect).createNativeQuery(session);
            long totalSize = Long.valueOf(countNativeQuery.uniqueResult().toString());

            return new Page<>(jobs, pageable.getPageSize(), pageable.getPageNumber(), totalSize);
        });
    }

    @Override
    public List<Job> listByStatus(EnumSet<JobStatus> status) {
        return execute(session -> session.createQuery("from Job where status in :status", Job.class)
            .setParameterList("status", status)
            .list());
    }

    @Override
    public Job get(String path, Long number) {
        return execute((Session session) -> {
            String where = "job.node_path=:name AND job.build_number=:build_number";
            Builder builder = ObjectUtil.deepCopy(QUERY_BUILDER);
            builder.where(where)
                .parameter("name", path)
                .parameter("build_number", number);
            NativeQuery nativeQuery = builder.createNativeQuery(session).setResultSetMapping("MappingJobResult");

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
