package com.flowci.core.agent.domain;

import java.util.Map;
import java.util.Optional;

public abstract class CmdStdLog {

    public static final String ID_HEADER = "id";

    public static final String STEP_ID_HEADER = "stepId";

    public static Optional<String> getFromHeader(Map<String, Object> src, String header) {
        if (src == null) {
            return Optional.empty();
        }

        Object id = src.get(header);
        if (id == null) {
            return Optional.empty();
        }

        return Optional.of(id.toString());
    }
}
