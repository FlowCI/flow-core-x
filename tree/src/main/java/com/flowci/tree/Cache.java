package com.flowci.tree;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
public class Cache implements Serializable {

    private String key;

    private List<String> paths;
}
