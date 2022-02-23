package com.flowci.core.git.controller;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.git.domain.GitConfig;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public abstract class Request {

    @Getter
    @Setter
    public static class SaveOptions {

        @NotNull
        private GitSource source;

        @NotEmpty
        private String secret;

        public GitConfig toGitConfig() {
            return new GitConfig(source, secret);
        }
    }

}
