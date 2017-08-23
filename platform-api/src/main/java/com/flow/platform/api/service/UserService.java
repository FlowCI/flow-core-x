package com.flow.platform.api.service;

import com.flow.platform.api.domain.User;

import java.util.List;

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

    /**
     * Check the format of the email
     *
     * @param email
     * @return
     */
    Boolean checkEmailFormatIsPass(String email);

    /**
     * Check the format of the user_name
     *
     * @param userName
     * @return
     */
    Boolean checkUserNameFormatIsPass(String userName);

    /**
     * Check the format of the password
     *
     * @param password
     * @return
     */
    Boolean checkPasswordFormatIsPass(String password);

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
}
