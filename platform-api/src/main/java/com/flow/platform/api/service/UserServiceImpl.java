package com.flow.platform.api.service;

import com.flow.platform.api.dao.UserDao;
import com.flow.platform.api.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author liangpengyv
 */
@Service(value = "userService")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Override
    public String loginByEmail(String email, String password) {
        if (!emailIsExist(email)) {
            return "{ \"login_status\" : \"email_is_not_exist\" }";
        }

        if (!passwordOfEmailIsTrue(email, password)) {
            return "{ \" login_status\" : \"password_fault\" }";
        }

        return "{ \"login_status\" : \"success\" }";
    }

    @Override
    public String loginByUserName(String userName, String password) {
        if (!userNameIsExist(userName)) {
            return "{ \"login_status\" : \"user_name_is_not_exist\" }";
        }

        if (!passwordOfUserNameIsTrue(userName, password)) {
            return "{ \"login_status\" : \"password_fault\" }";
        }

        return "{ \"login_status\" : \"success\" }";
    }

    @Override
    public String register(String email, String userName, String password) {
        if (emailIsExist(email)) {
            return "{ \"register_status\" : \"email_already_exist\" }";
        }
        if (userNameIsExist(userName)) {
            return "{ \"register_status\" : \"user_name_already_exist\" }";
        }

        User user = new User();

        user.setEmail(email);
        user.setUserName(userName);
        user.setPassword(password);
        user.setFlowAuth("");
        user.setRoleId("");
        user.setToken("");

        if (create(user)) {
            return "{ \"register_status\" : \"success\" }";
        } else {
            return "{ \"register_status\" : \"database_insert_exception\" }";
        }
    }

    @Override
    public Boolean emailIsExist(String email) {
        return userDao.emailIsExist(email);
    }

    @Override
    public Boolean userNameIsExist(String userName) {
        return userDao.userNameIsExist(userName);
    }

    @Override
    public Boolean passwordOfEmailIsTrue(String email, String password) {
        return userDao.passwordOfEmailIsTrue(email, password);
    }

    @Override
    public Boolean passwordOfUserNameIsTrue(String userName, String password) {
        return userDao.passwordOfUserNameIsTrue(userName, password);
    }

    @Override
    public Boolean create(User user) {
        try {
            userDao.save(user);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
