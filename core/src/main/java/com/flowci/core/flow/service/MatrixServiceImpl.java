/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.flow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.common.helper.StringHelper;
import com.flowci.core.common.helper.DateHelper;
import com.flowci.core.flow.dao.MatrixItemDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.MatrixCounter;
import com.flowci.core.flow.domain.MatrixItem;
import com.flowci.core.flow.domain.MatrixType;
import com.flowci.core.flow.event.FlowDeletedEvent;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.service.PluginService;
import com.flowci.common.exception.NotFoundException;
import com.flowci.tree.NodeTree;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

/**
 * @author yang
 */
@Slf4j
@Service
public class MatrixServiceImpl implements MatrixService {

    private static final TypeReference<List<MatrixType>> StatsTypeRef = new TypeReference<>() {
    };

    private final Map<String, MatrixType> defaultTypes = new HashMap<>(5);

    @Value("classpath:default_statistic_type.json")
    private Resource defaultTypeJsonFile;

    private final ObjectMapper objectMapper;

    private final MatrixItemDao matrixItemDao;

    private final YmlService ymlService;

    private final PluginService pluginService;

    public MatrixServiceImpl(ObjectMapper objectMapper, MatrixItemDao matrixItemDao, YmlService ymlService, PluginService pluginService) {
        this.objectMapper = objectMapper;
        this.matrixItemDao = matrixItemDao;
        this.ymlService = ymlService;
        this.pluginService = pluginService;
    }

    @PostConstruct
    public void loadDefaultTypes() {
        try {
            List<MatrixType> types = objectMapper.readValue(defaultTypeJsonFile.getInputStream(), StatsTypeRef);

            for (MatrixType t : types) {
                defaultTypes.put(t.getName(), t);
            }

        } catch (IOException e) {
            log.warn(e.getMessage());
        }
    }

    @EventListener
    public void onJobStatusChange(JobStatusChangeEvent event) {
        Job job = event.getJob();

        if (!job.isDone()) {
            return;
        }

        MatrixType t = defaultTypes.get(MatrixType.JOB_STATUS);
        MatrixItem item = t.createEmptyItem();
        item.getCounter().put(job.getStatus().name(), 1.0F);

        int day = DateHelper.toIntDay(job.getCreatedAt());
        add(job.getFlowId(), day, item.getType(), item.getCounter());
    }

    @EventListener
    public void onFlowDelete(FlowDeletedEvent event) {
        matrixItemDao.deleteByFlowId(event.getFlow().getId());
    }

    @Override
    public Map<String, MatrixType> defaultTypes() {
        return defaultTypes;
    }

    @Override
    public List<MatrixType> getStatsType(Flow flow) {
        List<MatrixType> list = new LinkedList<>(defaultTypes.values());
        NodeTree tree = ymlService.getTree(flow.getId());

        for (String pluginName : tree.getPlugins()) {
            try {
                Plugin plugin = pluginService.get(pluginName);
                list.addAll(plugin.getMeta().getMatrixTypes());
            } catch (NotFoundException ignore) {

            }
        }

        return list;
    }

    @Override
    public List<MatrixItem> list(String flowId, String type, int fromDay, int toDay) {
        Sort sort = Sort.by(Sort.Direction.ASC, "day");

        if (StringHelper.hasValue(type)) {
            return matrixItemDao.findByFlowIdAndTypeDayBetween(flowId, type, fromDay, toDay, sort);
        }

        return matrixItemDao.findByFlowIdDayBetween(flowId, fromDay, toDay, sort);
    }

    @Override
    public List<MatrixItem> list(Collection<String> flowIdList, String type, int day) {
        return matrixItemDao.findAllByFlowIdInAndDayAndType(flowIdList, day, type);
    }

    @Override
    public MatrixItem get(String flowId, String type, int day) {
        Optional<MatrixItem> item = matrixItemDao.findByFlowIdAndDayAndType(flowId, day, type);
        if (item.isPresent()) {
            return item.get();
        }
        throw new NotFoundException("Statistic data cannot found");
    }

    @Override
    @Transactional
    public MatrixItem add(String flowId, int day, String type, MatrixCounter counter) {
        MatrixItem totalItem = getItem(flowId, MatrixItem.ZERO_DAY, type);
        totalItem.plusDayCounter(counter);
        totalItem.plusOneToday();

        MatrixItem dayItem = getItem(flowId, day, type);
        dayItem.plusDayCounter(counter);
        dayItem.plusOneToday();

        dayItem.setTotal(totalItem.getCounter());
        dayItem.setNumOfTotal(totalItem.getNumOfToday());

        matrixItemDao.saveAll(Arrays.asList(dayItem, totalItem));
        return dayItem;
    }

    private MatrixItem getItem(String flowId, int day, String type) {
        Optional<MatrixItem> optional = matrixItemDao.findByFlowIdAndDayAndType(flowId, day, type);

        return optional.orElseGet(() -> new MatrixItem()
                .setDay(day)
                .setFlowId(flowId)
                .setType(type));
    }
}
