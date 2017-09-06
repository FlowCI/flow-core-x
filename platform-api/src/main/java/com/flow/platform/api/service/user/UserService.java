package com.flow.platform.api.service.user;

import com.flow.platform.api.domain.request.LoginForm;
import com.flow.platform.api.domain.user.User;

import java.util.List;
import java.util.Set;

/**
 * @author liangpengyv
 */
public interface UserService {

    /**
     * Find user by email
     */
    User findByEmail(String email);

    /**
     * Login
     */
    String login(LoginForm loginForm);

    /**
     * Register user to roles
     *
     * @param roles role name set, or null for not set to role
     */
    User register(User user, Set<String> roles);

    /**
     * Delete a user
     */
    void delete(List<String> emailList);
}
