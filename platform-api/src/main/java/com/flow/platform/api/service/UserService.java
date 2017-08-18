package com.flow.platform.api.service;

import com.flow.platform.api.domain.User;

/**
 * @author liangpengyv
 */
public interface UserService {

    /**
     * Login by email
     *
     * @param email
     * @param password
     * @return
     */
    String loginByEmail(String email, String password);

    /**
     * Login by user_name
     *
     * @param userName
     * @param password
     * @return
     */
    String loginByUserName(String userName, String password);

    /**
     * Register
     *
     * @param userName
     * @param password
     * @return
     */
    String register(String email, String userName, String password);

    /**
     * Verify email is exist
     *
     * @param email
     * @return
     */
    Boolean emailIsExist(String email);

    /**
     * Verify user_name is exist
     *
     * @param userName
     * @return
     */
    Boolean userNameIsExist(String userName);

    /**
     * Verify password of email
     *
     * @param email
     * @param password
     * @return
     */
    Boolean passwordOfEmailIsTrue(String email, String password);

    /**
     * Verify password of user_name
     *
     * @param userName
     * @param password
     * @return
     */
    Boolean passwordOfUserNameIsTrue(String userName, String password);

    /**
     * Create a new user info
     *
     * @param user
     * @return
     */
    Boolean create(User user);
}
