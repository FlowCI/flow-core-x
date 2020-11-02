package com.flowci.tree;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Cache {

    private String key;

    private List<String> paths;
}
