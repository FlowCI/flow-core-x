package com.flowci.domain;

import com.flowci.domain.tree.CacheOption;
import com.flowci.domain.tree.StepNode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StepNodeTest {

    @Test
    void whenGivenStepWithParent_thenShouldReturnDataIncludingParent() {
        StepNode root = StepNode.builder()
                .bash("hello from root")
                .pwsh("hello pwsh from root")
                .exports(Set.of("VAR_ROOT"))
                .secrets(Set.of("root-secret"))
                .condition("condition from root")
                .timeout(10)
                .cache(CacheOption.builder()
                        .key("report")
                        .paths(List.of("/path/a", "/path/b"))
                        .build())
                .build();

        StepNode parent = StepNode.builder()
                .parent(root)
                .bash("hello from parent")
                .pwsh("hello pwsh from parent")
                .exports(Set.of("VAR_PARENT_1", "VAR_PARENT_2"))
                .configs(Set.of("parent-config"))
                .condition("condition from parent")
                .timeout(5)
                .cache(CacheOption.builder()
                        .key("jar")
                        .paths(List.of("/path/b", "/java/jar"))
                        .build())
                .build();

        StepNode child = StepNode.builder()
                .parent(parent)
                .bash("hello from child")
                .pwsh("hello pwsh from child")
                .exports(new HashSet<>())
                .configs(Set.of("child-config-1", "child-config-2"))
                .condition("condition from child")
                .cache(CacheOption.builder()
                        .key("report")
                        .paths(List.of("/path/c"))
                        .build())
                .build();

        shouldFetchBashList(child);
        shouldFetchPwshList(child);
        shouldFetchMostClosedTimeOut(child);
        shouldFetchDefaultRetryWhenNoDefinition(child);
        shouldFetchExportList(child);
        shouldFetchSecretList(child);
        shouldFetchConfigList(child);
        shouldFetchConditionList(child);
        shouldFetchMostClosedCacheOptionSet(child);
    }

    private static void shouldFetchConfigList(StepNode step) {
        // when: fetch config list
        var configs = step.fetchConfig();
        assertEquals(3, configs.size());

        // then:
        assertTrue(configs.contains("child-config-1"));
        assertTrue(configs.contains("child-config-1"));
        assertTrue(configs.contains("parent-config"));
    }

    private static void shouldFetchExportList(StepNode step) {
        // when: fetch exports list
        var exports = step.fetchExports();
        assertEquals(3, exports.size());

        // then:
        assertTrue(exports.contains("VAR_ROOT"));
        assertTrue(exports.contains("VAR_PARENT_1"));
        assertTrue(exports.contains("VAR_PARENT_2"));
    }

    private static void shouldFetchSecretList(StepNode step) {
        // when: fetch secret list
        var secrets = step.fetchSecrets();
        assertEquals(1, secrets.size());

        // then:
        assertTrue(secrets.contains("root-secret"));
    }

    private static void shouldFetchPwshList(StepNode step) {
        // when: fetch pwsh list
        var pwshList = step.fetchPwsh();
        assertEquals(3, pwshList.size());

        // then:
        assertEquals("hello pwsh from root", pwshList.get(0));
        assertEquals("hello pwsh from parent", pwshList.get(1));
        assertEquals("hello pwsh from child", pwshList.get(2));
    }

    private void shouldFetchBashList(StepNode step) {
        // when: fetch bash list
        var bashList = step.fetchBash();
        assertEquals(3, bashList.size());

        // then:
        assertEquals("hello from root", bashList.get(0));
        assertEquals("hello from parent", bashList.get(1));
        assertEquals("hello from child", bashList.get(2));
    }

    private static void shouldFetchConditionList(StepNode step) {
        // when: fetch condition list
        var conditions = step.fetchCondition();
        assertEquals(3, conditions.size());

        // then:
        assertEquals("condition from root", conditions.get(0));
        assertEquals("condition from parent", conditions.get(1));
        assertEquals("condition from child", conditions.get(2));
    }

    private static void shouldFetchMostClosedCacheOptionSet(StepNode step) {
        // when: fetch condition list
        var options = step.fetchCacheOption();
        assertEquals(2, options.size());

        // then:
        var iter = options.iterator();

        var childCacheOpt = iter.next();
        assertEquals("report", childCacheOpt.getKey());
        assertEquals(1, childCacheOpt.getPaths().size());
        assertEquals("/path/c", childCacheOpt.getPaths().get(0));

        var parentCacheOpt = iter.next();
        assertEquals("jar", parentCacheOpt.getKey());
        assertEquals(2, parentCacheOpt.getPaths().size());
        assertEquals("/path/b", parentCacheOpt.getPaths().get(0));
        assertEquals("/java/jar", parentCacheOpt.getPaths().get(1));
    }

    private void shouldFetchMostClosedTimeOut(StepNode step) {
        assertEquals(5, step.fetchTimeout(100));
    }

    private void shouldFetchDefaultRetryWhenNoDefinition(StepNode step) {
        assertEquals(0, step.fetchRetry(0));
    }
}
