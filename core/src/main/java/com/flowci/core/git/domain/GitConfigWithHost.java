package com.flowci.core.git.domain;

import com.flowci.core.common.domain.GitSource;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document(collection = "git_config")
@NoArgsConstructor
public class GitConfigWithHost extends GitConfig {

    private String host;

    public GitConfigWithHost(GitSource source, String secret, String host) {
        super(source, secret);
        this.host = host;
    }
}
