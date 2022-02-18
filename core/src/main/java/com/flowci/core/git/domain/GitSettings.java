package com.flowci.core.git.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

@Getter
@Setter
public class GitSettings {

    public static final String Key = "_git_settings_";

    @Id
    private String id;

    @Indexed(unique = true)
    private String key;

    private String gitHubTokenSecret;
}
