package com.flowci.core.git.dao;

import com.flowci.core.git.domain.GitSettings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GitSettingsDao extends MongoRepository<GitSettings, String> {

    Optional<GitSettings> findByKey(String key);
}
