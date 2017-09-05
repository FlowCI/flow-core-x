package com.flow.platform.api.service.user;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.domain.request.LoginForm;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.security.token.TokenGenerator;
import com.flow.platform.api.util.StringEncodeUtil;
import com.flow.platform.api.security.token.JwtTokenGenerator;
import com.flow.platform.core.exception.IllegalParameterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value(value = "${expiration.duration}")
    private long expirationDuration;

    @Autowired
    private TokenGenerator tokenGenerator;

    @Override
    public String login(LoginForm loginForm) {
        String emailOrUsername = loginForm.getEmailOrUsername();
        String password = loginForm.getPassword();
        if (checkEmailFormatIsPass(emailOrUsername)) {
            // login by email
            return loginByEmail(emailOrUsername, password);
        }
        // else login by username
        return loginByUsername(emailOrUsername, password);
    }

    @Override
    public void register(User user) {
        String errMsg = "Illegal register request parameter: ";

        // Check format
        if (!checkEmailFormatIsPass(user.getEmail())) {
            throw new IllegalParameterException(errMsg + "email format false");
        }
        if (!checkUsernameFormatIsPass(user.getUsername())) {
            throw new IllegalParameterException(errMsg + "username format false");
        }
        if (!checkPasswordFormatIsPass(user.getPassword())) {
            throw new IllegalParameterException(errMsg + "password format false");
        }

        // Validate database
        if (emailIsExist(user.getEmail())) {
            throw new IllegalParameterException(errMsg + "email already exist");
        }
        if (usernameIsExist(user.getUsername())) {
            throw new IllegalParameterException(errMsg + "username already exist");
        }

        // Insert the user info into the database
        String passwordForMD5 = StringEncodeUtil.encodeByMD5(user.getPassword(), AppConfig.DEFAULT_CHARSET.name());
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
    public User findByEmail(String email){
        return userDao.get(email);
    }

    /**
     * Login by email
     */
    private String loginByEmail(String email, String password) {
        String errMsg = "Illegal login request parameter: ";

        // Check format
        if (!checkPasswordFormatIsPass(password)) {
            throw new IllegalParameterException(errMsg + "password format false");
        }

        // Validate database
        String passwordForMD5 = StringEncodeUtil.encodeByMD5(password, AppConfig.DEFAULT_CHARSET.name());
        if (!emailIsExist(email)) {
            throw new IllegalParameterException(errMsg + "email is not exist");
        }
        if (!passwordOfEmailIsTrue(email, passwordForMD5)) {
            throw new IllegalParameterException(errMsg + "password fault");
        }

        // Login success, return token
        return tokenGenerator.create(email, expirationDuration);
    }



    /**
     * Login by username
     */
    private String loginByUsername(String username, String password) {
        String errMsg = "Illegal login request parameter: ";

        // Check format
        if (!checkUsernameFormatIsPass(username)) {
            throw new IllegalParameterException(errMsg + "username format false");
        }
        if (!checkPasswordFormatIsPass(password)) {
            throw new IllegalParameterException(errMsg + "password format false");
        }

        // Validate database
        String passwordForMD5 = StringEncodeUtil.encodeByMD5(password, AppConfig.DEFAULT_CHARSET.name());
        if (!usernameIsExist(username)) {
            throw new IllegalParameterException(errMsg + "username is not exist");
        }
        if (!passwordOfUsernameIsTrue(username, passwordForMD5)) {
            throw new IllegalParameterException(errMsg + "password fault");
        }

        // Login success, return token
        String email = userDao.getEmailBy("username", username);
        return tokenGenerator.create(email, expirationDuration);
    }

    /**
     * Check the format of the email
     */
    private Boolean checkEmailFormatIsPass(String email) {
        if (email == null || email.trim().equals("")) {
            return false;
        }
        if (!Pattern.compile("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$").matcher(email).matches()) {
            return false;
        }
        return true;
    }

    /**
     * Check the format of the username
     */
    private Boolean checkUsernameFormatIsPass(String username) {
        if (username == null || username.trim().equals("")) {
            return false;
        }
        if (!Pattern.compile("^\\w{5,20}$").matcher(username).matches()) {
            return false;
        }
        return true;
    }

    /**
     * Check the format of the password
     */
    private Boolean checkPasswordFormatIsPass(String password) {
        if (password == null || password.trim().equals("")) {
            return false;
        }
        if (!Pattern.compile("^\\w{5,20}$").matcher(password).matches()) {
            return false;
        }
        return true;
    }

    /**
     * Verify email is exist
     */
    private Boolean emailIsExist(String email) {
        return userDao.emailIsExist(email);
    }

    /**
     * Verify username is exist
     */
    private Boolean usernameIsExist(String username) {
        return userDao.usernameIsExist(username);
    }

    /**
     * Verify password of email
     */
    private Boolean passwordOfEmailIsTrue(String email, String password) {
        return userDao.passwordOfEmailIsTrue(email, password);
    }

    /**
     * Verify password of username
     */
    private Boolean passwordOfUsernameIsTrue(String username, String password) {
        return userDao.passwordOfUsernameIsTrue(username, password);
    }
}
