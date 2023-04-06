package com.flowci.parser.v2;

import com.flowci.parser.TestUtil;
import com.flowci.parser.v2.yml.FlowYml;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class YmlParserTest {

    @Test
    void whenGivenYAML_thenShouldParseFlowYaml() throws IOException {
        var yml = TestUtil.loadContent("v2/flow.yml");
        var flow = YmlParser.load(yml);

        shouldParserFlowYmlProperties(flow);
    }

    private void shouldParserFlowYmlProperties(FlowYml flow) {
        assertEquals("root", flow.getName());
        assertEquals(2, flow.getVersion());

        var flowVars = flow.getVars();
        assertEquals(2, flowVars.size());
        assertEquals("echo hello", flowVars.get("FLOW_WORKSPACE"));
        assertEquals("echo version", flowVars.get("FLOW_VERSION"));

        assertEquals("return $FLOWCI_GIT_BRANCH == \"develop\" || $FLOWCI_GIT_BRANCH == \"master\";\n", flow.getCondition());

        var dockerOptions = flow.getDocker();
        assertEquals("helloworld:0.1", dockerOptions.getImage());

        var agents = flow.getAgents();
        assertEquals(3, agents.size());
        assertEquals("dev", agents.get(0));
        assertEquals("stage", agents.get(1));
        assertEquals("prod", agents.get(2));
    }
}
