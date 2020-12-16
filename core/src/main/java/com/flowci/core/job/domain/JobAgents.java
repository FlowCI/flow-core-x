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
    private final Map<String, Set<String>> agents = new HashMap<>();

    /**
     * Add or update agent, node
     */
    public void save(String agentId, Node node) {
        Set<String> set = agents.computeIfAbsent(agentId, k -> new HashSet<>());
        FlowNode flow = node.getParentFlowNode();
        set.add(flow.getPathAsString());
    }

    /**
     * Remove flow path only
     */
    public void remove(String agentId, Node node) {
        agents.computeIfPresent(agentId, (key, map) -> {
            FlowNode flow = node.getParentFlowNode();
            map.remove(flow.getPathAsString());
            return map;
        });
    }

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
