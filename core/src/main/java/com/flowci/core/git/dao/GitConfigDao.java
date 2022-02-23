package com.flowci.core.git.dao;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.git.domain.GitConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GitConfigDao extends MongoRepository<GitConfig, String> {

    Optional<GitConfig> findBySource(GitSource source);
}
