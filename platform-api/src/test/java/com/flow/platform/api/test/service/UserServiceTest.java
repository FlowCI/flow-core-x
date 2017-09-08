package com.flow.platform.api.test.service;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.request.LoginParam;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.api.service.user.UserService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.StringEncodeUtil;
import com.google.common.collect.Sets;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author liangpengyv
 */
public class UserServiceTest extends TestBase {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private NodeService nodeService;

    private User user;

    private LoginParam loginForm;

    private final Set<String> roles = Sets.newHashSet("ROLE_ADMIN", "ROLE_USER");

    @Before
    public void beforeTest() {
        user = new User();
        user.setEmail("liangpengyv@fir.im");
        user.setUsername("liangpengyv");
        user.setPassword("liangpengyv");

        // createOrUpdate roles
        for (String role : roles) {
            roleService.create(role, null);
        }
    }

    @Test
    public void should_list_user_with_flow_and_role() {
        // given: user with roles
        userService.register(user, roles);

        // given: flows with created by
        Flow flow = nodeService.createEmptyFlow("flow-test");
        flow.setCreatedBy("liangpengyv@fir.im");
        flowDao.update(flow);

        // when: load path list by createdBy
        List<User> users = userService.list(true, true);
        Assert.assertEquals(1, users.size());

        // then:
        User user = users.get(0);
        Assert.assertEquals(1, user.getFlows().size());
        Assert.assertEquals("flow-test", user.getFlows().get(0));

        Assert.assertEquals(2, user.getRoles().size());
    }

    @Test
    public void should_login_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), AppConfig.DEFAULT_CHARSET.name()));
        userDao.save(user);
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));

        loginForm = new LoginParam();
        loginForm.setEmailOrUsername("liangpengyv@fir.im");
        loginForm.setPassword("liangpengyv");
        String msg = userService.login(loginForm);
        Assert.assertTrue(msg.length() > 20);

        loginForm.setEmailOrUsername("liangpengyv");
        Assert.assertTrue(msg.length() > 20);
    }

    @Test
    public void should_register_success() {
        userService.register(user, null);
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));
    }

    @Test
    public void should_delete_user_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), AppConfig.DEFAULT_CHARSET.name()));
        userDao.save(user);
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));

        List<String> emailList = new LinkedList<>();
        emailList.add("liangpengyv@fir.im");
        userService.delete(emailList);
        Assert.assertNull(userDao.get("liangpengyv@fir.im"));
    }
}
