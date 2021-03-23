package com.flowci.core.job.domain;

import com.flowci.tree.FlowNode;
import com.flowci.tree.Node;
import com.flowci.tree.NodePath;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@ToString(of = "agents")
@Document(collection = "job_agent")
public final class JobAgent {

    @Id
    private String id; // job id

    private String flowId;

    /**
     * agent1 -> flow
     * flow/parallel/sub_1
     */
    private Map<String, Set<String>> agents = new HashMap<>();

    public JobAgent(String jobId, String flowId) {
        this.id = jobId;
        this.flowId = flowId;
    }

    public Collection<String> all() {
        return agents.keySet();
    }

    /**
     * All busy agents, which are occupied by flow and assigned to step
     */
    public Collection<String> allBusyAgents(Collection<Step> ongoingSteps) {
        Set<String> busy = new HashSet<>(agents.size());
        this.agents.forEach((k, v) -> {
            if (v.isEmpty()) {
                return;
            }

            for(Step s : ongoingSteps) {
                if (s.hasAgent() && s.getAgentId().equals(k)) {
                    busy.add(k);
                    return;
                }
            }
        });
        return busy;
    }

    public boolean isOccupiedByFlow(String agentId) {
        Set<String> flows = agents.get(agentId);
        return flows != null && !flows.isEmpty();
    }

    /**
     * Add or update agent, node
     * that means the agent will be used in flows
     */
    public void save(String agentId, FlowNode flow) {
        Set<String> set = agents.computeIfAbsent(agentId, k -> new HashSet<>());
        set.add(flow.getPathAsString());
    }

    /**
     * Remove when last node of flow was executed
     * that means release the agent from flow
     */
    public void remove(String agentId, FlowNode flow) {
        agents.computeIfPresent(agentId, (key, map) -> {
            map.remove(flow.getPathAsString());
            return map;
        });
    }

    /**
     * Remove agent from job
     */
    public void remove(String agentId) {
        agents.remove(agentId);
    }

    /**
     * Get agent if an agent was occupied within same flow
     */
    public Optional<String> getAgent(Node node) {
        FlowNode flow = node.getParent(FlowNode.class);
        for (Map.Entry<String, Set<String>> entry : agents.entrySet()) {
            String agentId = entry.getKey();
            Set<String> set = entry.getValue();

            if (set.contains(flow.getPathAsString())) {
                return Optional.of(agentId);
            }
        }
        return Optional.empty();
    }

    /**
     * Call if getAgent returns empty
     * find candidate agents within job, but still need to check Selector
     */
    public List<String> getCandidates(Node node) {
        FlowNode flow = node.getParent(FlowNode.class);
        int flowDepth = flow.getPath().depth();

        List<String> candidates = new ArrayList<>(agents.size());

        for (Map.Entry<String, Set<String>> entry : agents.entrySet()) {
            String agentId = entry.getKey();
            List<NodePath> list = toSortedNodePathList(entry.getValue());

            // agent not occupied by any flow
            if (list.isEmpty()) {
                candidates.add(agentId);
                continue;
            }

            // agent is occupied in other parallel flow
            int maxPathDepth = list.get(0).depth();
            if (maxPathDepth >= flowDepth) {
                continue;
            }

            // find candidate agent from other flows (except parallel)
            for (NodePath p : list) {
                if (p.depth() != flowDepth) {
                    candidates.add(agentId);
                }
            }
        }

        return candidates;
    }

    private static List<NodePath> toSortedNodePathList(Set<String> set) {
        List<NodePath> list = new ArrayList<>(set.size());
        for (String s : set) {
            list.add(NodePath.create(s));
        }
        list.sort((o1, o2) -> Integer.compare(o2.depth(), o1.depth()));
        return list;
    }
}
