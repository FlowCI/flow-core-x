package com.flow.platform.api.service;

import com.flow.platform.api.dao.UserDao;
import com.flow.platform.api.domain.User;
import com.flow.platform.api.util.StringEncodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * @author liangpengyv
 */
@Service(value = "userService")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Override
    public String loginByEmail(String email, String password) {
        //Check format
        if (!checkEmailFormatIsPass(email)) {
            return "{ \"login_status\" : \"email_format_false\" }";
        }
        if (!checkPasswordFormatIsPass(password)) {
            return "{ \"login_status\" : \"password_format_false\" }";
        }

        //Validate database
        String passwordForMD5 = StringEncodeUtil.encodeByMD5(password, "UTF-8");
        if (!emailIsExist(email)) {
            return "{ \"login_status\" : \"email_is_not_exist\" }";
        }
        if (!passwordOfEmailIsTrue(email, passwordForMD5)) {
            return "{ \" login_status\" : \"password_fault\" }";
        }

        //Login success
        return "{ \"login_status\" : \"success\" }";
    }

    @Override
    public String loginByUserName(String userName, String password) {
        //Check format
        if (!checkUserNameFormatIsPass(userName)) {
            return "{ \"login_status\" : \"user_name_format_false\" }";
        }
        if (!checkPasswordFormatIsPass(password)) {
            return "{ \"login_status\" : \"password_format_false\" }";
        }

        //Validate database
        String passwordForMD5 = StringEncodeUtil.encodeByMD5(password, "UTF-8");
        if (!userNameIsExist(userName)) {
            return "{ \"login_status\" : \"user_name_is_not_exist\" }";
        }
        if (!passwordOfUserNameIsTrue(userName, passwordForMD5)) {
            return "{ \"login_status\" : \"password_fault\" }";
        }

        //Login success
        return "{ \"login_status\" : \"success\" }";
    }

    @Override
    public String register(User user) {
        //Check format
        if (!checkEmailFormatIsPass(user.getEmail())) {
            return "{ \"register_status\" : \"email_format_false\" }";
        }
        if (!checkUserNameFormatIsPass(user.getUserName())) {
            return "{ \"register_status\" : \"user_name_format_false\" }";
        }
        if (!checkPasswordFormatIsPass(user.getPassword())) {
            return "{ \"register_status\" : \"password_format_false\" }";
        }

        //Validate database
        if (emailIsExist(user.getEmail())) {
            return "{ \"register_status\" : \"email_already_exist\" }";
        }
        if (userNameIsExist(user.getUserName())) {
            return "{ \"register_status\" : \"user_name_already_exist\" }";
        }

        //Insert the user info into the database
        String passwordForMD5 = StringEncodeUtil.encodeByMD5(user.getPassword(), "UTF-8");
        user.setPassword(passwordForMD5);
        if (create(user)) {
            return "{ \"register_status\" : \"success\" }";
        } else {
            return "{ \"register_status\" : \"database_insert_exception\" }";
        }
    }

    @Override
    public Boolean checkEmailFormatIsPass(String email) {
        if (email == null || email.trim().equals("")) {
            return false;
        }
        if (!Pattern.compile("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$").matcher(email).matches()) {
            return false;
        }
        return true;
    }

    @Override
    public Boolean checkUserNameFormatIsPass(String userName) {
        if (userName == null || userName.trim().equals("")) {
            return false;
        }
        if (!Pattern.compile("^\\w{5,20}$").matcher(userName).matches()) {
            return false;
        }
        return true;
    }

    @Override
    public Boolean checkPasswordFormatIsPass(String password) {
        if (password == null || password.trim().equals("")) {
            return false;
        }
        if (!Pattern.compile("^\\w{5,20}$").matcher(password).matches()) {
            return false;
        }
        return true;
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
