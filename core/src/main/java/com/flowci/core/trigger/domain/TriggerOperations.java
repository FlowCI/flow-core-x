package com.flowci.core.trigger.domain;

import com.google.common.collect.ImmutableList;

import java.util.List;

public abstract class TriggerOperations {

    public static final String LIST = "list_notify";

    public static final String GET = "get_notify";

    public static final String SAVE = "create_or_update_notify";

    public static final String DELETE = "delete_notify";

    public static final List<String> ALL = ImmutableList.of(
            LIST,
            GET,
            SAVE,
            DELETE
    );
}
