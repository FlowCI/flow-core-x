package com.flowci.core.git.util;

import com.flowci.core.git.domain.GitCommit;
import com.flowci.util.StringHelper;

import java.util.Optional;

public abstract class CommitHelper {

    private final static String ChangeIdStr = "Change-Id:";

    public static Optional<String> getChangeId(GitCommit commit) {
        String message = commit.getMessage();
        if (!StringHelper.hasValue(message)) {
            return Optional.empty();
        }

        int i = message.lastIndexOf(ChangeIdStr);
        if (i == -1) {
            return Optional.empty();
        }

        int end = message.indexOf('\n', i);
        if (end == -1) {
            return Optional.of(message.substring(i + ChangeIdStr.length() + 1));
        }

        return Optional.of(message.substring(i + ChangeIdStr.length() + 1, end));
    }
}
