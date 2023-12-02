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

package com.flowci.core.flow.controller;

import com.flowci.core.common.helper.DateHelper;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.MatrixItem;
import com.flowci.core.flow.domain.MatrixType;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.MatrixService;
import com.flowci.common.exception.ArgumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

/**
 * @author yang
 */
@RestController
@RequestMapping("/flows")
public class MatrixController {

    private static final int MaxDays = 31;

    @Autowired
    private FlowService flowService;

    @Autowired
    private MatrixService matrixService;

    @GetMapping("/{name}/matrix/types")
    public List<MatrixType> types(@PathVariable String name) {
        Flow flow = flowService.get(name);
        return matrixService.getStatsType(flow);
    }

    @GetMapping("/{name}/matrix/total")
    public MatrixItem total(@PathVariable String name, @RequestParam String t) {
        Flow flow = flowService.get(name);
        return matrixService.get(flow.getId(), t, MatrixItem.ZERO_DAY);
    }

    @PostMapping("/matrix/batch/total")
    public List<MatrixItem> batchTotal(@RequestParam String t, @RequestBody Collection<String> flowIdList) {
        return matrixService.list(flowIdList, t, MatrixItem.ZERO_DAY);
    }

    @GetMapping("/{name}/matrix")
    public List<MatrixItem> list(@PathVariable String name,
                                 @RequestParam(required = false) String t,
                                 @RequestParam int from,
                                 @RequestParam int to) {

        if (isValidDuration(from, to)) {
            throw new ArgumentException("Illegal query argument");
        }

        Flow flow = flowService.get(name);
        return matrixService.list(flow.getId(), t, from, to);
    }

    private boolean isValidDuration(int from, int to) {
        Instant f = DateHelper.toInstant(from);
        Instant t = DateHelper.toInstant(to);

        if (f.isAfter(t)) {
            return false;
        }

        return !f.plus(MaxDays, ChronoUnit.DAYS).isAfter(t);
    }
}
