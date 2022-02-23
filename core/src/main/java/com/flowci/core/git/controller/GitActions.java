package com.flowci.core.git.controller;

import com.google.common.collect.ImmutableList;

import java.util.List;

public abstract class GitActions {

    public static final String LIST = "list_git_config";

    public static final String GET = "get_git_config";

    public static final String SAVE = "save_git_config";

    public static final String DELETE = "delete_git_config";

    public static final List<String> ALL = ImmutableList.of(
            LIST,
            GET,
            SAVE,
            DELETE
    );
}
