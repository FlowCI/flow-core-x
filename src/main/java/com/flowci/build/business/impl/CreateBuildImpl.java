package com.flowci.build.business.impl;

import com.flowci.build.business.CreateBuild;
import com.flowci.build.model.Build;
import com.flowci.build.model.BuildYaml;
import com.flowci.build.repo.BuildRepo;
import com.flowci.build.repo.BuildYamlRepo;
import com.flowci.build.repo.JobRepo;
import com.flowci.common.RequestContextHolder;
import com.flowci.common.model.Variables;
import com.flowci.flow.business.FetchFlow;
import com.flowci.flow.business.FetchFlowYamlContent;
import com.flowci.flow.model.Flow;
import com.flowci.yaml.business.ParseYaml;
import com.flowci.yaml.model.Command;
import com.flowci.yaml.model.Step;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@AllArgsConstructor
public class CreateBuildImpl implements CreateBuild {

    private final FetchFlow fetchFlow;
    private final FetchFlowYamlContent fetchFlowYamlContent;
    private final ParseYaml parseYamlV2;
    private final BuildRepo buildRepo;
    private final BuildYamlRepo buildYamlRepo;
    private final JobRepo jobRepo;
    private final RequestContextHolder requestContextHolder;

    @Override
    @Transactional
    public Build invoke(Long flowId, Build.Trigger trigger, @Nullable Variables inputs) {
        var flow = fetchFlow.invoke(flowId);

        var yaml = fetchFlowYamlContent.invoke(flowId, false);
        var yamlObj = parseYamlV2.invoke(yaml);
        var agentTags = yamlObj.getAgents() == null
                ? Set.<String>of()
                : new HashSet<>(yamlObj.getAgents());

        var build = new Build();
        build.setFlowId(flow.getId());
        build.setContext(toBuildVariables(flow, inputs));
        build.setTrigger(trigger);
        build.setStatus(Build.Status.CREATED);

        build.setCreatedBy(requestContextHolder.getUserId());
        build.setUpdatedBy(requestContextHolder.getUserId());
        buildRepo.save(build);

        var buildYaml = new BuildYaml();
        buildYaml.setId(build.getId());
        buildYaml.setYaml(yaml);
        buildYaml.setCreatedBy(build.getCreatedBy());
        buildYaml.setUpdatedBy(build.getUpdatedBy());
        buildYamlRepo.save(buildYaml);


        log.info("build {} is created for flow {} with trigger {}", build.getBuildAlias(), flowId, trigger);
        return build;
    }

    private Variables toBuildVariables(Flow flow, Variables inputs) {
        if (inputs == null) {
            return flow.getVariables();
        }

        var variables = new Variables(flow.getVariables());
        variables.putAll(inputs); // inputs has top priority
        return variables;
    }
}
