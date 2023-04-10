package com.flowci.parser.v2;

import com.flowci.parser.TestUtil;
import com.flowci.parser.v2.yml.FlowYml;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExecutionGraphV2Test {

    @Test
    void whenGivenFlowYml_thenShouldCreateGraph() throws IOException {
        var yml = TestUtil.loadContent("v2/flow.yml");
        var flowYml = YmlParser.parse(yml);

        var graph = new ExecutionGraphV2(flowYml);
        Assertions.assertNotNull(graph);

        graphNodeShouldBeConnected(graph);
    }

    private void graphNodeShouldBeConnected(ExecutionGraphV2 graph) {
        var root = graph.getRoot();
        assertEquals(FlowYml.DEFAULT_NAME, root.getName());
        assertEquals(FlowYml.DEFAULT_NAME, root.getPath());

        var rootChildren = root.getChildren();
        assertEquals(1, rootChildren.size());

        var step1 = rootChildren.get(0);
        assertEquals("step-1", step1.getName());
        assertEquals("root/step-1", step1.getPath());
        assertEquals(1, step1.getParents().size());
        assertEquals(root, step1.getParents().get(0));

        var step1Children = step1.getChildren();
        assertEquals(2, step1Children.size());

        var step2 = step1Children.get(0);
        assertEquals("step-2", step2.getName());
        assertEquals("root/step-2", step2.getPath());
        assertEquals(1, step2.getParents().size());
        assertEquals(step1, step2.getParents().get(0));

        var step2Children = step2.getChildren();
        assertEquals(1, step2Children.size());

        var step2_1 = step2Children.get(0);
        assertEquals("step-2-1", step2_1.getName());
        assertEquals("root/step-2/step-2-1", step2_1.getPath());
        assertEquals(1, step2_1.getChildren().size());
        assertEquals(1, step2_1.getParents().size());
        assertEquals(step2, step2_1.getParents().get(0));

        var step2_2 = step2_1.getChildren().get(0);
        assertEquals("step-2-2", step2_2.getName());
        assertEquals("root/step-2/step-2-2", step2_2.getPath());
        assertEquals(1, step2_2.getParents().size());
        assertEquals(step2_1, step2_2.getParents().get(0));

        var step3 = step1Children.get(1);
        assertEquals("step-3", step3.getName());
        assertEquals("root/step-3", step3.getPath());
        assertEquals(1, step3.getParents().size());
        assertEquals(step1, step3.getParents().get(0));

        var step3Children = step3.getChildren();
        assertEquals(1, step3Children.size());

        var step3_1 = step3Children.get(0);
        assertEquals("step-3-1", step3_1.getName());
        assertEquals("root/step-3/step-3-1", step3_1.getPath());
        assertEquals(1, step3_1.getParents().size());
        assertEquals(step3, step3_1.getParents().get(0));

        var step4 = step3_1.getChildren().get(0);
        assertEquals(step3_1.getChildren().get(0), step2_2.getChildren().get(0));

        assertEquals("step-4", step4.getName());
        assertEquals("root/step-4", step4.getPath());
        assertEquals(2, step4.getParents().size());
        assertEquals(step2_2, step4.getParents().get(0));
        assertEquals(step3_1, step4.getParents().get(1));

        var step4Children = step4.getChildren();
        assertEquals(1, step4Children.size());

        var step5 = step4Children.get(0);
        assertEquals("step-5", step5.getName());
        assertEquals("root/step-5", step5.getPath());
        assertEquals(1, step5.getParents().size());
        assertEquals(step4, step5.getParents().get(0));
    }
}
