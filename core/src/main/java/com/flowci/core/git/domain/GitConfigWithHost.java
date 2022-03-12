package com.flowci.core.git.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document(collection = "git_config")
public class GitConfigWithHost extends GitConfig {

    private String host;
}
