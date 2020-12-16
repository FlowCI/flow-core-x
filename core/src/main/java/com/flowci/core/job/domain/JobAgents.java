package com.flowci.core.job.domain;

import com.flowci.tree.FlowNode;
import com.flowci.tree.Node;
import com.flowci.tree.NodePath;

import java.util.*;

public final class JobAgents {

    /**
     * agent1 -> flow
     *           flow/parallel/sub_1
     */
    private Map<String, Set<String>> agents = new HashMap<>();

    public Collection<String> all() {
        return agents.keySet();
    }

    /**
     * Add or update agent, node
     * that means the agent will be used in flows
     */
    public void save(String agentId, FlowNode flow) {
        Set<String> set = agents.computeIfAbsent(agentId, k -> new HashSet<>());
        set.add(flow.getPathAsString());
    }

    public boolean isEmpty() {
        return agents.isEmpty();
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
     * Get agent if an agent was occupied within same flow
     */
    public Optional<String> getAgent(Node node) {
        FlowNode flow = node.getParentFlowNode();
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
        FlowNode flow = node.getParentFlowNode();
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
