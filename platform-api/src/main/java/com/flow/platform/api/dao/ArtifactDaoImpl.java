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

import com.flow.platform.api.domain.Artifact;
import com.flow.platform.core.dao.AbstractBaseDao;
import com.flow.platform.core.domain.Page;
import com.flow.platform.core.domain.Pageable;
import java.math.BigInteger;
import java.util.List;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

/**
 * @author yh@firim
 */
@Repository
public class ArtifactDaoImpl extends AbstractBaseDao<Integer, Artifact> implements ArtifactDao {

    @Override
    protected Class<Artifact> getEntityClass() {
        return Artifact.class;
    }

    @Override
    protected String getKeyName() {
        return "id";
    }

    @Override
    public List<Artifact> list(BigInteger jobId) {
        return execute(session -> session
            .createQuery("from Artifact where jobId = :id", getEntityClass())
            .setParameter("id", jobId)
            .list());
    }

    @Override
    public Page<Artifact> list(BigInteger jobId, Pageable pageable) {
        return execute(session -> {

                TypedQuery query = session
                    .createQuery("from Artifact where jobId = :id", getEntityClass())
                    .setParameter("id", jobId);

                return buildPage(query, pageable);

            }
        );
    }
}
