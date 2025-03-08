package com.flowci.build.business.impl;

import com.flowci.build.business.CreateBuild;
import com.flowci.build.model.Build;
import com.flowci.build.model.BuildYaml;
import com.flowci.build.model.Job;
import com.flowci.build.repo.BuildRepo;
import com.flowci.build.repo.BuildYamlRepo;
import com.flowci.build.repo.JobRepo;
import com.flowci.common.RequestContextHolder;
import com.flowci.common.model.Variables;
import com.flowci.flow.business.FetchFlow;
import com.flowci.flow.business.FetchFlowYamlContent;
import com.flowci.flow.model.Flow;
import com.flowci.yaml.business.ParseYaml;
import com.flowci.yaml.model.v2.CommandV2;
import com.flowci.yaml.model.v2.DockerV2;
import com.flowci.yaml.model.v2.StepV2;
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
    private final ParseYaml<DockerV2, StepV2, CommandV2> parseYamlV2;
    private final BuildRepo buildRepo;
    private final BuildYamlRepo buildYamlRepo;
    private final JobRepo jobRepo;
    private final RequestContextHolder requestContextHolder;

    @Override
    @Transactional
    public Build invoke(Long flowId, Build.Trigger trigger, @Nullable Variables inputs) {
        var flow = fetchFlow.invoke(flowId);

        var yaml = fetchFlowYamlContent.invoke(flowId, false);
        var flowYamlObj = parseYamlV2.invoke(yaml);

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

        createJobs(flowYamlObj.getNext(), build, new HashSet<>());
        log.info("build {} is created for flow {} with trigger {}", build.getBuildAlias(), flowId, trigger);
        return build;
    }

    private void createJobs(List<StepV2> steps, Build build, Set<Job.Id> saved) {
        for (var step : steps) {
            var id = new Job.Id(build.getId(), step.getName());
            if (saved.contains(id)) {
                continue;
            }

            var next = step.getNext();

            var job = new Job();
            job.setId(id);
            job.setNext(next.stream().map((StepV2::getName)).toArray(String[]::new));
            job.setStatus(Job.Status.CREATED);
            job.setAgentTags(step.getAgents().toArray(new String[0]));
            job.setCreatedBy(build.getCreatedBy());
            job.setUpdatedBy(build.getUpdatedBy());

            jobRepo.save(job);
            saved.add(id);

            createJobs(step.getNext(), build, saved);
        }
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
