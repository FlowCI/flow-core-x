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

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.node.NodeStatus;
import com.flow.platform.api.dao.util.JobConvertUtil;
import com.flow.platform.core.dao.AbstractBaseDao;
import java.math.BigInteger;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
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

    private final static String TEMPLATE = " job.*, nr.* ";


    @Override
    public List<Job> list() {
        return execute((Session session) -> {
            NativeQuery nativeQuery = session.createNativeQuery(
                "select * from job as job left join node_result as nr"
                    + "  on job.node_path=nr.node_path and job.id=nr.job_id"
            )
                .setResultSetMapping("MappingJobResult");
            List<Object[]> objects = nativeQuery.list();
            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public List<Job> list(List<String> sessionIds, NodeStatus nodeStatus) {
        return execute((Session session) -> {
            NativeQuery nativeQuery = session.createNativeQuery(
                "select * from job as job left join node_result as nr"
                    + "  on job.node_path=nr.node_path and job.id=nr.job_id"
                    + "  where "
                    + "    job.session_id in (:sessionIds) and nr.status=:status"
            )
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
            NativeQuery nativeQuery = session.createNativeQuery(
                "select * from job as job left join node_result as nr"
                    + "  on job.node_path=nr.node_path and job.id=nr.job_id"
                    + "  where "
                    + "    job.id=:id"
            )
                .setParameter("id", key)
                .setResultSetMapping("MappingJobResult");
            Object[] objects = (Object[]) nativeQuery.uniqueResult();
            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public List<Job> listLatest(List<String> nodeNames) {
        return execute((Session session) -> {
            NativeQuery nativeQuery = session.createNativeQuery(
                "select * from job as job left join node_result as nr"
                    + "  on job.node_path=nr.node_path and job.id=nr.job_id"
                    + "  where "
                    + "    id in (select max(id) from Job where nodeName in (:names) group by nodePath)"
            )
                .setParameter("names", nodeNames)
                .setResultSetMapping("MappingJobResult");
            List<Object[]> objects = nativeQuery.list();
            return JobConvertUtil.convert(objects);
        });
    }


    @Override
    public List<Job> list(String nodePath) {
        return execute((Session session) -> {


            NativeQuery nativeQuery = session.createNativeQuery(
                "select * from job as job left join node_result as nr"
                    + "  on job.node_path=nr.node_path and job.id=nr.job_id"
                    + "  where "
                    + "    job.node_path=:path"
            )
                .setParameter("path", nodePath)
                .setResultSetMapping("MappingJobResult");
            List<Object[]> objects = nativeQuery.list();
            return JobConvertUtil.convert(objects);
        });
    }

    @Override
    public Job get(String flowName, Integer number) {
        return execute((Session session) -> {

            NativeQuery nativeQuery = session.createNativeQuery(
                "select * from job as job left join node_result as nr"
                    + "  on job.node_path=nr.node_path and job.id=nr.job_id"
                    + "  where "
                    + "    job.node_name=:name and job.number=:number"
            )
                .setParameter("name", flowName)
                .setParameter("number", number)
                .setResultSetMapping("MappingJobResult");
            Object[] objects = (Object[]) nativeQuery.uniqueResult();
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
