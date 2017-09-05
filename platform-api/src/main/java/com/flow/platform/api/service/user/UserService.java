package com.flow.platform.api.service.user;

import com.flow.platform.api.domain.request.LoginForm;
import com.flow.platform.api.domain.user.User;

import java.util.List;

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
     * Register
     */
    User register(User user);

    /**
     * Delete a user
     */
    void delete(List<String> emailList);
}
