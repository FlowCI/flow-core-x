package com.flow.platform.api.dao;

import com.flow.platform.api.domain.User;
import com.flow.platform.core.dao.BaseDao;

import java.util.List;

/**
 * @author liangpengyv
 */
public interface UserDao extends BaseDao<String, User> {

    /**
     * Verify email is exist
     *
     * @param email
     * @return
     */
    Boolean emailIsExist(String email);

    /**
     * Verify username is exist
     *
     * @param username
     * @return
     */
    Boolean usernameIsExist(String username);

    /**
     * Verify password of email
     *
     * @param email
     * @param password
     * @return
     */
    Boolean passwordOfEmailIsTrue(String email, String password);

    /**
     * Verify password of username
     *
     * @param username
     * @param password
     * @return
     */
    Boolean passwordOfUsernameIsTrue(String username, String password);

    /**
     * Get email(PRIMARY_KEY) by a "where" condition
     *
     * @param whereWhatFieldName
     * @param whereWhatFieldValue
     * @return
     */
    String getEmailBy(String whereWhatFieldName, String whereWhatFieldValue);

    /**
     * Delete a group users through email
     *
     * @param emailList
     */
    void deleteList(List<String> emailList);

    /**
     * Switch the role_id of the user
     *
     * @param emailList
     * @param roleId
     */
    void switchUserRoleIdTo(List<String> emailList, String roleId);
}
