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

package com.flow.platform.api.service;

import com.flow.platform.api.dao.ArtifactDao;
import com.flow.platform.api.domain.Artifact;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.core.exception.IllegalParameterException;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */
@Service
public class ArtifactServiceImpl implements ArtifactService {

    @Autowired
    private ArtifactDao artifactDao;

    @Autowired
    private NodeService nodeService;

    @Override
    public Artifact create(Artifact artifact) {
        if (Strings.isNullOrEmpty(artifact.getFlow())) {
            throw new IllegalParameterException("Parameter flow is missing");
        }

        NodeTree flow = nodeService.find(artifact.getFlow());
        if (Objects.isNull(flow)) {
            throw new IllegalParameterException("Parameter flow is error, not found flow");
        }

        return artifactDao.save(artifact);
    }

    @Override
    public Artifact get(Integer id) {
        return artifactDao.get(id);
    }

    @Override
    public List<Artifact> list(String flow) {

        NodeTree nodeTree = nodeService.find(flow);
        if (Objects.isNull(nodeTree)) {
            throw new IllegalParameterException("Parameter flow is error, not found flow");
        }

        return artifactDao.list(flow);
    }
}