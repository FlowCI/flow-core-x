package com.flowci.yaml.business;

import com.flowci.yaml.model.Command;
import com.flowci.yaml.model.Docker;
import com.flowci.yaml.model.Flow;
import com.flowci.yaml.model.Step;

public interface ParseYaml<D extends Docker, S extends Step<D, S, C>, C extends Command> {
    Flow<D, S, C> invoke(String yaml);
}
