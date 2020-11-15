package com.flowci.tree;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class Cache implements Serializable {

    private String key;

    private List<String> paths;
}
