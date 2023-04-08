package com.flowci.parser.v2;

import com.flowci.exception.YmlException;
import com.flowci.parser.TestUtil;
import com.flowci.parser.v2.yml.FlowYml;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class YmlParserTest {

    @Test
    void whenGivenYAML_thenShouldParseFlowYaml() throws IOException {
        var yml = TestUtil.loadContent("v2/flow.yml");
        var flow = YmlParser.load(yml);

        shouldParseFlowYmlProperties(flow);
        shouldParseStepYmlProperties(flow);
        shouldConvertToFlowNode(flow);
    }

    @Test
    void whenConvertToFlowNode_thenShouldThrowExceptionIfStepDepthOverTwo() throws IOException {
        var yml = TestUtil.loadContent("v2/flow-over-2-depth.yml");
        var flowYml = YmlParser.load(yml);
        Assertions.assertThrows(YmlException.class, flowYml::convert);
    }

    private void shouldConvertToFlowNode(FlowYml flowYml) {
        var flow = flowYml.convert();
        assertNotNull(flow);
        assertEquals(5, flow.getSteps().size());

        var step2 = flow.getSteps().get(1);
        assertEquals(2, step2.getSteps().size());

        var step3 = flow.getSteps().get(2);
        assertEquals(1, step3.getSteps().size());
    }

    private void shouldParseStepYmlProperties(FlowYml flow) {
        var steps = flow.getSteps();
        assertNotNull(steps);
        assertEquals(5, steps.size());

        var keyIter = steps.keySet().iterator();

        // verify: step 1
        var step1 = steps.get("step-1");
        assertNotNull(step1);
        assertEquals("step-1", keyIter.next());
        assertEquals("echo step 1", step1.getBash());

        // verify: step 2
        var step2 = steps.get("step-2");
        assertNotNull(step2);
        assertEquals("step-2", keyIter.next());
        assertEquals("echo step 2", step2.getBash());
        assertEquals(1, step2.getDependencies().size());
        assertEquals("step-1", step2.getDependencies().get(0));

        // verify: step 2 children
        var step2ChildrenSteps = step2.getSteps();
        assertNotNull(step2ChildrenSteps);
        assertEquals(2, step2ChildrenSteps.size());

        // verify: step 2 child 1
        var step2_1 = step2ChildrenSteps.get("step-2-1");
        assertEquals("echo step-2-1", step2_1.getBash());

        // verify: step 2 child 2
        var step2_2 = step2ChildrenSteps.get("step-2-2");
        assertEquals("echo step-2-2", step2_2.getBash());

        // verify: step 3
        var step3 = steps.get("step-3");
        assertNotNull(step3);
        assertEquals("step-3", keyIter.next());
        assertEquals("echo step 3", step3.getBash());
        assertEquals(1, step3.getDependencies().size());
        assertEquals("step-1", step3.getDependencies().get(0));

        // verify: step 3 children
        var step3ChildrenSteps = step3.getSteps();
        assertNotNull(step3ChildrenSteps);
        assertEquals(1, step3ChildrenSteps.size());

        // verify: step 3 child 1
        var step3_1 = step3ChildrenSteps.get("step-3-1");
        assertEquals("echo step-3-1", step3_1.getBash());

        // verify: step 4
        var step4 = steps.get("step-4");
        assertNotNull(step4);
        assertEquals("step-4", keyIter.next());
        assertEquals("echo step 4", step4.getBash());

        assertEquals(2, step4.getDependencies().size());
        assertEquals("step-2", step4.getDependencies().get(0));
        assertEquals("step-3", step4.getDependencies().get(1));

        assertEquals(1, step4.getArtifacts().size());
        assertEquals("jar", step4.getArtifacts().get(0).getName());
        assertEquals(2, step4.getArtifacts().get(0).getPaths().size());
        assertEquals("/path/jar", step4.getArtifacts().get(0).getPaths().get(0));
        assertEquals("/var/pwd", step4.getArtifacts().get(0).getPaths().get(1));

        // verify: step-5
        var step5 = steps.get("step-5");
        assertNotNull(step5);
        assertEquals("step-5", keyIter.next());
        assertEquals("echo step 5", step5.getBash());

        assertEquals(1, step5.getDependencies().size());
        assertEquals("step-4", step5.getDependencies().get(0));
    }

    private void shouldParseFlowYmlProperties(FlowYml flow) {
        assertEquals("root", flow.getName());
        assertEquals(2, flow.getVersion());

        var flowVars = flow.getVars();
        assertEquals(2, flowVars.size());
        assertEquals("echo hello", flowVars.get("FLOW_WORKSPACE"));
        assertEquals("echo version", flowVars.get("FLOW_VERSION"));

        assertEquals("return $FLOWCI_GIT_BRANCH == \"develop\" || $FLOWCI_GIT_BRANCH == \"master\";\n", flow.getCondition());

        var dockers = flow.getDockers();
        assertEquals(1, dockers.size());
        assertEquals("helloworld:0.1", dockers.get(0).getImage());

        var agents = flow.getAgents();
        assertEquals(3, agents.size());
        assertEquals("dev", agents.get(0));
        assertEquals("stage", agents.get(1));
        assertEquals("prod", agents.get(2));
    }
}
