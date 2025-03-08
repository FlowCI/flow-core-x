package com.flowci.build.business;

import com.flowci.SpringTestWithDB;
import com.flowci.build.model.Build;
import com.flowci.build.model.Job;
import com.flowci.build.repo.JobRepo;
import com.flowci.common.model.Variables;
import com.flowci.flow.business.CreateFlow;
import com.flowci.flow.business.UpdateFlowYamlContent;
import com.flowci.flow.model.CreateFlowParam;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateBuildTest extends SpringTestWithDB {

    @Value("classpath:yaml/v2_success_parallel_steps.yaml")
    private Resource rawYamlResource;

    @Autowired
    private CreateFlow createFlow;

    @Autowired
    private UpdateFlowYamlContent updateFlowYamlContent;

    @Autowired
    private CreateBuild createBuild;

    @Autowired
    private JobRepo jobRepo;

    @Test
    void givenFlow_whenCreating_thenBuildIsCreated() throws IOException {
        // given:
        var yaml = rawYamlResource.getContentAsString(Charset.defaultCharset());
        var flow = createFlow.invoke(new CreateFlowParam("test_flow", null, null));
        updateFlowYamlContent.invoke(flow.getId(), Base64.getEncoder().encodeToString(yaml.getBytes()));

        // when: create build
        var inputs = new Variables();
        inputs.put("v1", "hello");
        inputs.put("v2", "world");
        var build = createBuild.invoke(flow.getId(), Build.Trigger.API, inputs);

        // then:
        assertEquals(flow.getId(), build.getFlowId());
        assertEquals(Build.Trigger.API, build.getTrigger());
        assertEquals("hello", build.getContext().get("v1"));
        assertEquals("world", build.getContext().get("v2"));

        var step_abc = jobRepo.findById(new Job.Id(build.getId(), "step_abc")).orElseThrow();
        assertArrayEquals(new String[]{}, step_abc.getNext());

        var step_1 = jobRepo.findById(new Job.Id(build.getId(), "step_1")).orElseThrow();
        assertArrayEquals(new String[]{"step_2_A_1", "step_2_B"}, step_1.getNext());

        var step_2_a_1 = jobRepo.findById(new Job.Id(build.getId(), "step_2_A_1")).orElseThrow();
        assertArrayEquals(new String[]{"step_2_A_2"}, step_2_a_1.getNext());

        var step_2_a_2 = jobRepo.findById(new Job.Id(build.getId(), "step_2_A_2")).orElseThrow();
        assertArrayEquals(new String[]{"step_3"}, step_2_a_2.getNext());

        var step_2_B = jobRepo.findById(new Job.Id(build.getId(), "step_2_B")).orElseThrow();
        assertArrayEquals(new String[]{"step_3"}, step_2_B.getNext());

        var step_3 = jobRepo.findById(new Job.Id(build.getId(), "step_3")).orElseThrow();
        assertArrayEquals(new String[]{}, step_3.getNext());
    }
}
