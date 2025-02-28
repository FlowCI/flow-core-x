package com.flowci.yaml.business;

import com.flowci.yaml.model.Flow;

public interface ParseYaml {
    Flow invoke(String yaml);
}
