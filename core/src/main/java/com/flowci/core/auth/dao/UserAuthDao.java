package com.flowci.core.auth.dao;

import com.flowci.core.auth.domain.UserAuth;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAuthDao extends CustomUserAuthDao, MongoRepository<UserAuth, String> {

    Optional<UserAuth> findByEmail(String email);

    Optional<UserAuth> findByToken(String token);

    void deleteByEmail(String email);
}
