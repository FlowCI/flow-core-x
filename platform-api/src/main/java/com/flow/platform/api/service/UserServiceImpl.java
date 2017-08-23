package com.flow.platform.api.service;

import com.flow.platform.api.dao.UserDao;
import com.flow.platform.api.domain.User;
import com.flow.platform.api.util.StringEncodeUtil;
import com.flow.platform.core.exception.IllegalParameterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author liangpengyv
 */
@Service(value = "userService")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Override
    public String loginByEmail(String email, String password) throws IllegalParameterException {
        String errMsg = "Illegal login request parameter: ";

        //Check format
        if (!checkEmailFormatIsPass(email)) {
            throw new IllegalParameterException(errMsg + "email format false");
        }
        if (!checkPasswordFormatIsPass(password)) {
            throw new IllegalParameterException(errMsg + "password format false");
        }

        //Validate database
        String passwordForMD5 = StringEncodeUtil.encodeByMD5(password, "UTF-8");
        if (!emailIsExist(email)) {
            throw new IllegalParameterException(errMsg + "email is not exist");
        }
        if (!passwordOfEmailIsTrue(email, passwordForMD5)) {
            throw new IllegalParameterException(errMsg + "password fault");
        }

        //Login success
        return "token";
    }

    @Override
    public String loginByUserName(String userName, String password) throws IllegalParameterException {
        String errMsg = "Illegal login request parameter: ";

        //Check format
        if (!checkUserNameFormatIsPass(userName)) {
            throw new IllegalParameterException(errMsg + "user name format false");
        }
        if (!checkPasswordFormatIsPass(password)) {
            throw new IllegalParameterException(errMsg + "password format false");
        }

        //Validate database
        String passwordForMD5 = StringEncodeUtil.encodeByMD5(password, "UTF-8");
        if (!userNameIsExist(userName)) {
            throw new IllegalParameterException(errMsg + "user name is not exist");
        }
        if (!passwordOfUserNameIsTrue(userName, passwordForMD5)) {
            throw new IllegalParameterException(errMsg + "password fault");
        }

        //Login success
        return "token";
    }

    @Override
    public void register(User user) throws IllegalParameterException {
        String errMsg = "Illegal register request parameter: ";

        //Check format
        if (!checkEmailFormatIsPass(user.getEmail())) {
            throw new IllegalParameterException(errMsg + "email format false");
        }
        if (!checkUserNameFormatIsPass(user.getUserName())) {
            throw new IllegalParameterException(errMsg + "user name format false");
        }
        if (!checkPasswordFormatIsPass(user.getPassword())) {
            throw new IllegalParameterException(errMsg + "password format false");
        }

        //Validate database
        if (emailIsExist(user.getEmail())) {
            throw new IllegalParameterException(errMsg + "email already exist");
        }
        if (userNameIsExist(user.getUserName())) {
            throw new IllegalParameterException(errMsg + "user name already exist");
        }

        //Insert the user info into the database
        String passwordForMD5 = StringEncodeUtil.encodeByMD5(user.getPassword(), "UTF-8");
        user.setPassword(passwordForMD5);
        userDao.save(user);
    }

    @Override
    public void delete(List<String> emailList) {
        userDao.deleteList(emailList);
    }

    @Override
    public void switchRole(List<String> emailList, String roleId) {
        userDao.switchUserRoleIdTo(emailList, roleId);
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
}
