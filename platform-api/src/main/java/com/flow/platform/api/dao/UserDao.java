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
