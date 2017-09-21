package com.flow.platform.api.service.user;

import com.flow.platform.api.domain.request.LoginParam;
import com.flow.platform.api.domain.response.UserListResponse;
import com.flow.platform.api.domain.user.User;

import java.util.List;
import java.util.Set;

/**
 * @author liangpengyv
 */
public interface UserService {

    /**
     * List all users
     */
    UserListResponse list(boolean withFlow, boolean withRole);

    /**
     * Find user by email
     */
    User findByEmail(String email);

    /**
     * Login
     */
    String login(LoginParam loginForm);

    /**
     * Register user to roles
     *
     * @param roles role name set, or null for not set to role
     */
    User register(User user, List<String> roles, boolean isSendEmail, List<String> flowsList);

    /**
     * Delete a user
     */
    void delete(List<String> emailList);

    /**
     * update user role
     */
    List<User> updateUserRole(List<String> emailList, List<String> roles);
}
