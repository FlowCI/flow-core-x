package com.flow.platform.api.dao.user;

import com.flow.platform.api.domain.user.User;
import com.flow.platform.core.dao.BaseDao;

import java.util.List;

/**
 * @author liangpengyv
 */
public interface UserDao extends BaseDao<String, User> {

    /**
     * Count total num of
     */
    Long count();

    /**
     * Find user by username
     */
    User getByUsername(String username);

    /**
     * Delete a group users through email
     */
    void delete(List<String> emailList);
}
