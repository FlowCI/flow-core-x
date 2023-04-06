package com.flowci.parser.v2.yml;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class FileOptionYml {

    private String name;

    private List<String> paths;
}
