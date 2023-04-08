package com.flowci.parser;

import com.flowci.domain.node.NodePath;
import com.flowci.domain.node.StepNode;

import java.util.Collection;

public interface Tree {

    /**
     * Find step node by string path
     *
     * @param path string path
     * @return step node
     */
    StepNode find(String path);

    /**
     * Find Step node by NodePath
     *
     * @param path node path
     * @return step node
     */
    StepNode find(NodePath path);

    /**
     * Find next step nodes from current
     *
     * @param current current node
     * @param post is find next post step nodes
     * @return next step node list
     */
    Collection<StepNode> next(StepNode current, boolean post);

    /**
     * Find next step nodes after current one
     *
     * @param current current node
     * @return next step node list after current node
     */
    Collection<StepNode> skip(StepNode current);

    /**
     * Find previous step node list
     *
     * @param current current node
     * @return previous step node list
     */
    Collection<StepNode> previous(StepNode current);
}
