package com.flowci.core.agent.domain;

import com.flowci.core.common.rabbit.RabbitOperations;

import java.util.Objects;
import java.util.Optional;

public abstract class CmdStdLog {

    public static final String ID_HEADER = "id";

    public static final String STEP_ID_HEADER = "stepId";

    public static Optional<String> getFromHeader(RabbitOperations.Message message, String header) {
        if (!message.hasHeader()) {
            return Optional.empty();
        }

        Object id = message.getHeaders().get(header);
        if (Objects.isNull(id)) {
            return Optional.empty();
        }

        return Optional.of(id.toString());
    }
}
