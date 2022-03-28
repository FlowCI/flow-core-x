package com.flowci.core.git.domain;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.secret.domain.Secret;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "git_config")
public class GitConfig extends Mongoable {

    @Indexed(unique = true)
    private GitSource source;

    private String secret;

    @Transient
    private Secret secretObj;

    public GitConfig(GitSource source, String secret) {
        this.source = source;
        this.secret = secret;
    }
}
