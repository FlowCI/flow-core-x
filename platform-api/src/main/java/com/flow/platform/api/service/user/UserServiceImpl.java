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
import com.flow.platform.api.domain.user.SysRole;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.security.token.TokenGenerator;
import com.flow.platform.api.service.CurrentUser;
import com.flow.platform.api.service.MessageService;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.util.SmtpUtil;
import com.flow.platform.api.util.StringEncodeUtil;
import com.flow.platform.core.exception.IllegalParameterException;

import com.flow.platform.util.ExceptionUtil;
import com.flow.platform.util.Logger;
import com.flow.platform.util.http.HttpURL;
import java.io.StringWriter;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
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
public class UserServiceImpl extends CurrentUser implements UserService {

    private final static Logger LOGGER = new Logger(UserService.class);

    private final static String REGISTER_TEMPLATE_SUBJECT = "邀请您加入项目 [ flow.ci ]";

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
    private VelocityEngine velocityEngine;

    @Value(value = "${api.user.expire}")
    private long expirationDuration;

    @Value("${domain.web}")
    private String webDomain;

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
                // for find flow by createdBy
                // List<String> paths = nodeService.listFlowPathByUser(Lists.newArrayList(user.getEmail()))
                // user.setFlows(paths);
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

        String originPassword = user.getPassword();

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
        user.setCreatedBy(currentUser().getEmail());
        user = userDao.save(user);

        if (isSendEmail) {
            sendEmail(currentUser(), user, originPassword);
        }

        if (flowsList.size() > 0) {
            for (String rootPath : flowsList) {
                Flow flow = (Flow) nodeService.find(rootPath);
                if (flow != null) {
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
        if (emailList.contains(currentUser().getEmail())){
            throw new IllegalParameterException("params emails include yourself email, not delete");
        }
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
    public List<User> updateUserRole(List<String> emailList, List<String> roles) {
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
    public User findByEmail(String email) {
        return userDao.get(email);
    }

    @Override
    public Long adminUserCount() {
        Role role = roleService.find(SysRole.ADMIN.name());
        return userRoleDao.numOfUser(role.getId());
    }

    @Override
    public Long usersCount() {
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

    private void sendEmail(User currentUser, User toUser, String originPassword) {
        EmailSettingContent emailSettingContent = (EmailSettingContent) messageService.find(MessageType.EMAIl);

        if (emailSettingContent == null) {
            LOGGER.warnMarker("sendMessage", "Email settings not found");
            return;
        }

        String text = buildEmailTemplate(currentUser, toUser, originPassword);

        try {
            // send email to creator
            SmtpUtil.sendEmail(emailSettingContent, toUser.getEmail(), REGISTER_TEMPLATE_SUBJECT, text);
            LOGGER.traceMarker("sendMessage", "send message to %s success", toUser.getEmail());

        } catch (Throwable e) {
            LOGGER.traceMarker("sendMessage", "send email to user error : %s",
                ExceptionUtil.findRootCause(e).getMessage());
        }
    }

    private String buildEmailTemplate(User fromUser, User toUser, String originPassword) {
        try {
            final String detailUrl = HttpURL.build(webDomain).append("/admin/members/list").toString();

            Template template = velocityEngine.getTemplate("email/register_email.vm");
            VelocityContext velocityContext = new VelocityContext();
            velocityContext.put("fromUser", fromUser);
            velocityContext.put("toUser", toUser);
            velocityContext.put("password", originPassword);
            velocityContext.put("detailUrl", detailUrl);
            StringWriter stringWriter = new StringWriter();
            template.merge(velocityContext, stringWriter);
            return stringWriter.toString();
        } catch (Throwable e) {
            LOGGER.warn("sendMessage", "send message to user error : %s",
                ExceptionUtil.findRootCause(e).getMessage());
            return null;
        }
    }

}
