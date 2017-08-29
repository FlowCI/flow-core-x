package com.flow.platform.api.service.user;

import com.flow.platform.api.domain.request.LoginForm;
import com.flow.platform.api.domain.user.User;

import java.util.List;

/**
 * @author liangpengyv
 */
public interface UserService {

    /**
     * Login
     *
     * @param loginForm
     * @return
     */
    String login(LoginForm loginForm);

    /**
     * Register
     *
     * @param user
     */
    void register(User user);

    /**
     * Delete a user
     *
     * @param emailList
     */
    void delete(List<String> emailList);

    /**
     * Update role_id of user
     *
     * @param emailList
     * @param roleId
     */
    void switchRole(List<String> emailList, String roleId);
}
