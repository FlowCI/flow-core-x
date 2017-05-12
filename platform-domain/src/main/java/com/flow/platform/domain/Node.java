package com.flow.platform.domain;

import java.io.Serializable;
import java.util.List;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */
public class Node implements Serializable {

    /**
     * Node unique id
     */
    private String id;

    /**
     * Node name
     */
    private String name;

    /**
     * Node target working zone
     */
    private String zone;

    /**
     * Node target working machine, unique in the zone
     */
    private String machine;

    /**
     * Node execution sequence
     */
    private Integer sequence = 0;

    /**
     * Parent node
     */
    private Node parent;

    /**
     * Children
     */
    private List<Node> children;

}
