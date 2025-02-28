package com.flowci.yaml.model;

import java.util.List;
import java.util.Map;

public interface Docker {

    String getImage();

    String getNetwork();

    List<String> getPorts();

    List<String> getEntrypoint();

    List<String> getCommand();

    Map<String, String> getEnvironment();

    Boolean getIsRuntime();
}
