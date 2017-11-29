package com.flow.platform.api.test.service;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.response.LoginResponse;
import com.flow.platform.api.domain.user.SysRole;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.api.service.user.UserService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.StringEncodeUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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

    private List<String> roles = new ArrayList<>();

    @Before
    public void beforeTest() {
        user = new User();
        user.setEmail("liangpengyv@fir.im");
        user.setUsername("liangpengyv");
        user.setPassword("liangpengyv");

        // create roles
        roles.add("ROLE_ADMIN");
        roles.add("ROLE_USER");
        for (String role : roles) {
            roleService.create(role, null);
        }
    }

    @Test
    public void should_list_user_with_flow_and_role() {
        // given: user with roles
//        userService.register(user, roles, false, null);

        userService.register(user, roles, false,
            Lists.newArrayList(createFlow().getPath()));

        nodeService.createEmptyFlow("flow_test");

        List<User> users = userService.list(true, true);
        Assert.assertEquals(2, users.size());

        // then:
        User user = users.get(1);
        Assert.assertEquals(1, user.getFlows().size());
        Assert.assertEquals("test", user.getFlows().get(0));

        Assert.assertEquals(2, user.getRoles().size());
    }

    @Test
    public void should_init_sys_user_success() {

        // init role
        for (SysRole role : SysRole.values()) {
            try {
                roleService.create(role.name(), "System default role");
            } catch (Throwable ignore) {
                // ignore duplication
            }
        }

        User superUser = new User();
        superUser.setUsername("willadmin");
        superUser.setEmail("yh@fir.im");
        superUser.setPassword("123456");
        // when init sys user
        userService.initSysUser(superUser, ImmutableList.of(SysRole.ADMIN.name()), Collections.emptyList());
        User user = userDao.getByUsername("willadmin");

        // then: user is not null
        Assert.assertNotNull(user);

        // then: email is equal
        Assert.assertEquals("yh@fir.im", user.getEmail());

        // when: update password
        superUser.setPassword("qwertyui");
        userService.initSysUser(superUser, ImmutableList.of(SysRole.ADMIN.name()), Collections.emptyList());

        user = userDao.getByUsername("willadmin");
        Assert.assertNotNull(user);
        Assert.assertEquals("yh@fir.im", user.getEmail());
        Assert.assertEquals("willadmin", user.getUsername());

        // then: password is equal
        Assert.assertEquals(StringEncodeUtil.encodeByMD5("qwertyui", AppConfig.DEFAULT_CHARSET.name()),
            user.getPassword());
    }

    @Test
    public void should_login_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), AppConfig.DEFAULT_CHARSET.name()));
        userDao.save(user);
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));

        LoginResponse msg = userService.login("liangpengyv@fir.im", "liangpengyv");
        Assert.assertTrue(msg.getToken().length() > 20);
    }

    @Test
    public void should_register_success() {
        userService.register(user, roles, false, ImmutableList.of(createFlow().getPath()));
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

    private Node createFlow() {
        String path = "test";
        return nodeService.createEmptyFlow(path);
    }
}
