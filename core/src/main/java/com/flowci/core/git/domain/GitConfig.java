package com.flowci.core.git.domain;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.common.domain.Mongoable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

@Getter
@Setter
@NoArgsConstructor
public class GitConfig extends Mongoable {

    @Indexed(unique = true)
    private GitSource source;

    private String secret;

    public GitConfig(GitSource source, String secret) {
        this.source = source;
        this.secret = secret;
    }
}
