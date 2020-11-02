package com.flowci.tree.yml;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CacheYml {

    private String key;

    private List<String> paths;
}
