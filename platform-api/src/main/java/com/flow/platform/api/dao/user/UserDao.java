package com.flow.platform.api.dao.user;

import com.flow.platform.api.domain.user.User;
import com.flow.platform.core.dao.BaseDao;

import java.util.List;

/**
 * @author liangpengyv
 */
public interface UserDao extends BaseDao<String, User> {

    /**
     * Verify email is exist
     */
    Boolean emailIsExist(String email);

    /**
     * Verify username is exist
     */
    Boolean usernameIsExist(String username);

    /**
     * Verify password of email
     */
    Boolean passwordOfEmailIsTrue(String email, String password);

    /**
     * Verify password of username
     */
    Boolean passwordOfUsernameIsTrue(String username, String password);

    /**
     * Get email(PRIMARY_KEY) by a "where" condition
     */
    String getEmailBy(String whereWhatFieldName, String whereWhatFieldValue);

    /**
     * Delete a group users through email
     */
    void deleteList(List<String> emailList);
}
