package com.flowci.core.git.controller;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.git.domain.GitConfig;
import com.flowci.core.git.domain.GitConfigWithHost;
import com.flowci.util.StringHelper;
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

        private String host;

        @NotEmpty
        private String secret;

        public GitConfig toGitConfig() {
            if (StringHelper.hasValue(host)) {
                return new GitConfigWithHost(source, secret, host);
            }

            return new GitConfig(source, secret);
        }
    }

}
