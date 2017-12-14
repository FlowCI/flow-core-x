package com.flow.platform.api.service.user;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.dao.user.UserFlowDao;
import com.flow.platform.api.dao.user.UserRoleDao;
import com.flow.platform.api.domain.EmailSettingContent;
import com.flow.platform.api.domain.MessageType;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.domain.response.LoginResponse;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author liangpengyv
 */
@Service
@Transactional
public class UserServiceImpl extends CurrentUser implements UserService {

    private final static Logger LOGGER = new Logger(UserService.class);

    private final static String REGISTER_TEMPLATE_SUBJECT = "邀请您加入项目 [ flow.ci ]";

    private final Map<String, User> loginUserMap = new HashMap<>();

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
                user.setFlows(userFlowDao.listByEmail(user.getEmail()));
            }
        }

        return users;
    }

    @Override
    public List<User> list(Collection<String> emails) {
        return userDao.list(emails);
    }

    @Override
    public LoginResponse login(String emailOrUsername, String rawPassword) {
        // try to get user via email or username
        User user = userDao.get(emailOrUsername);
        if (user == null) {
            user = userDao.getByUsername(emailOrUsername);
        }

        if (user == null) {
            throw new IllegalParameterException("Illegal email or username");
        }

        String md5Password = StringEncodeUtil.encodeByMD5(rawPassword, AppConfig.DEFAULT_CHARSET.name());
        if (!Objects.equals(md5Password, user.getPassword())) {
            throw new IllegalParameterException("Illegal password for user: " + emailOrUsername);
        }

        // create token and save to memory
        String token = tokenGenerator.create(user.getEmail(), expirationDuration);
        user.setRoles(roleService.list(user));
        loginUserMap.put(token, user);

        return new LoginResponse(token, user);
    }

    @Override
    public User register(User user, List<String> roles, boolean isSendEmail, List<String> flowsList) {
        String errMsg = "Illegal register request parameter: ";
        // check user params is legal
        checkUserInfoIsLegal(user);

        // Validate database
        User existed = userDao.get(user.getEmail());
        if (existed != null) {
            throw new IllegalParameterException(errMsg + "email already exist");
        }

        existed = userDao.getByUsername(user.getUsername());
        if (existed != null) {
            throw new IllegalParameterException(errMsg + "username already exist");
        }

        String originPassword = user.getPassword();

        // Insert the user info into the database
        user.setPassword(encodePassword(user.getPassword()));
        user.setCreatedBy(currentUser().getEmail());
        user = userDao.save(user);

        if (isSendEmail) {
            sendEmail(currentUser(), user, originPassword);
        }

        assignRoleToUser(user, roles, flowsList);

        return user;
    }

    @Override
    public void changePassword(User user, String oldPassword, String newPassword) {
        if (oldPassword != null && !Objects.equals(user.getPassword(), encodePassword(oldPassword))) {
            throw new IllegalParameterException("The old password input is incorrect");
        }

        user.setPassword(encodePassword(newPassword));
        userDao.update(user);
    }

    @Override
    public void delete(List<String> emailList) {
        if (emailList.contains(currentUser().getEmail())) {
            throw new IllegalParameterException("params emails include yourself email, not delete");
        }
        // un-assign user from role and flow
        List<User> users = userDao.list(emailList);
        for (User user : users) {
            roleService.unAssign(user);
            userFlowService.unAssign(user);
        }

        // delete user
        userDao.delete(emailList);
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
    public User findByToken(String token) {
        return loginUserMap.get(token);
    }

    @Override
    public Long adminUserCount() {
        Role role = roleService.find(SysRole.ADMIN.name());
        return userRoleDao.numOfUser(role.getId());
    }

    @Override
    public Long usersCount() {
        return userDao.count();
    }

    private String encodePassword(String password) {
        return StringEncodeUtil.encodeByMD5(password, AppConfig.DEFAULT_CHARSET.name());
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

    private void assignRoleToUser(User user, List<String> roles, List<String> flowsList) {
        for (String rootPath : flowsList) {
            NodeTree nodeTree = nodeService.find(rootPath);
            if (nodeTree == null) {
                continue;
            }

            Node flow = nodeTree.root();
            if (flow == null) {
                continue;
            }

            userFlowService.assign(user, flow);
        }

        if (roles == null || roles.isEmpty()) {
            return;
        }

        // assign user to role
        for (String roleName : roles) {
            Role targetRole = roleService.find(roleName);
            roleService.assign(user, targetRole);
        }
    }

    private void checkUserInfoIsLegal(User user) {

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

    }

    private void checkPasswordAndUpdatePassword(User existed, String newPassword) {
        // Insert the user info into the database
        String passwordForMD5 = StringEncodeUtil.encodeByMD5(newPassword, AppConfig.DEFAULT_CHARSET.name());
        if (Objects.equals(existed.getPassword(), passwordForMD5)) {
            return;
        }

        existed.setPassword(passwordForMD5);
        userDao.update(existed);
    }

}
