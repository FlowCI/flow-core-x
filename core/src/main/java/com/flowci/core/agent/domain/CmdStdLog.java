package com.flowci.core.agent.domain;

import com.flowci.core.common.rabbit.RabbitOperations;

import java.util.Objects;
import java.util.Optional;

public abstract class CmdStdLog {

    public static final String ID_HEADER = "id";

    public static Optional<String> getId(RabbitOperations.Message message) {
        if (!message.hasHeader()) {
            return Optional.empty();
        }

        Object id = message.getHeaders().get(CmdStdLog.ID_HEADER);
        if (Objects.isNull(id)) {
            return Optional.empty();
        }

        return Optional.of(id.toString());
    }
}
