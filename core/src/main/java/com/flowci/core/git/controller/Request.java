package com.flowci.core.git.controller;

import com.flowci.common.helper.StringHelper;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.git.domain.GitConfig;
import com.flowci.core.git.domain.GitConfigWithHost;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

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
