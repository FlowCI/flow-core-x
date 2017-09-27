package com.flow.platform.api.service.user;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.dao.user.UserFlowDao;
import com.flow.platform.api.dao.user.UserRoleDao;
import com.flow.platform.api.domain.EmailSettingContent;
import com.flow.platform.api.domain.MessageType;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.request.LoginParam;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.RoleName;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.security.token.TokenGenerator;
import com.flow.platform.api.service.MessageService;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.util.SmtpUtil;
import com.flow.platform.api.util.StringEncodeUtil;
import com.flow.platform.core.exception.IllegalParameterException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author liangpengyv
 */
@Service(value = "userService")
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private TokenGenerator tokenGenerator;

    @Autowired
    private RoleService roleService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private UserFlowService userFlowService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserRoleDao userRoleDao;

    @Autowired
    private UserFlowDao userFlowDao;

    @Autowired
    protected ThreadLocal<User> currentUser;

    @Value(value = "${expiration.duration}")
    private long expirationDuration;

    @Override
    public List<User> list(boolean withFlow, boolean withRole) {
        List<User> users = userDao.list();

        if (!withFlow && !withRole) {
            return users;
        }

        for (User user : users) {
            if (withRole) {
                user.setRoles(roleService.list(user));
            }

            if (withFlow) {
                user.setFlows(userFlowDao.listByEmail(user.getEmail()));
            }

        }

        return users;
    }

    @Override
    public String login(LoginParam loginForm) {
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
    public User register(User user, List<String> roles, boolean isSendEmail, List<String> flowsList) {
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
        user.setCreatedBy(currentUser.get().getEmail());
        user = userDao.save(user);


        if (isSendEmail){
            EmailSettingContent emailSettingContent = (EmailSettingContent) messageService.find(MessageType.EMAIl);
            if (emailSettingContent != null){
                SmtpUtil.sendEmail(emailSettingContent, user.getEmail(), "邀请您加入项目 [ flow.ci ]",
                    "你已被邀请加入 FLOW.CI, 用户名:"+ user.getEmail() + "密码:" + user.getPassword());
            }
        }

        if (flowsList.size() > 0){
            for (String rootPath : flowsList){
                Flow flow = (Flow) nodeService.find(rootPath);
                if (flow != null){
                    userFlowService.assign(user, flow);
                }
            }
        }

        if (roles == null || roles.isEmpty()) {
            return user;
        }

        // assign user to role
        for (String roleName : roles) {
            Role targetRole = roleService.find(roleName);
            roleService.assign(user, targetRole);
        }

        return user;
    }

    @Override
    public void delete(List<String> emailList) {
        // un-assign user from role and flow
        List<User> users = userDao.list(emailList);
        for (User user : users) {
            roleService.unAssign(user);
            userFlowService.unAssign(user);
        }

        // delete user
        userDao.deleteList(emailList);
    }

    @Override
    public List<User> updateUserRole(List<String> emailList, List<String> roles){
        List<User> users = userDao.list(emailList);
        for (User user : users) {
            roleService.unAssign(user);
            for (String roleName : roles) {
                Role targetRole = roleService.find(roleName);
                roleService.assign(user, targetRole);
                user.setRoles(roleService.list(user));
                user.setFlows(userFlowDao.listByEmail(user.getEmail()));
            }
        }
        return users;
    }

    @Override
    public User findByEmail(String email){
        return userDao.get(email);
    }

    @Override
    public Long adminUserCount(){
        Role role = roleService.find(RoleName.ADMIN.getName());
        return userRoleDao.numOfUser(role.getId());
    }

    @Override
    public Long usersCount(){
        List<User> users = userDao.list();
        Long userCount = new Long(users.size());
        return userCount;
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
