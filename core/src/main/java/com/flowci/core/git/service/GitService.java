package com.flowci.core.git.service;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.git.domain.GitConfig;

import java.util.List;

public interface GitService {

    List<GitConfig> list();

    GitConfig save(GitConfig config);

    GitConfig get(GitSource source);

    void delete(GitSource source);
}
