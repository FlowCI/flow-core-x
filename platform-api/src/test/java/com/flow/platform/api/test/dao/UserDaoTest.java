package com.flow.platform.api.test.dao;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.StringEncodeUtil;
import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author liangpengyv
 */
public class UserDaoTest extends TestBase {

    @Autowired
    private UserDao userDao;

    private User user;

    @Before
    public void beforeTest() {
        user = new User();
        user.setEmail("liangpengyv@fir.im");
        user.setUsername("liangpengyv");
        user.setPassword(StringEncodeUtil.encodeByMD5("liangpengyv", AppConfig.DEFAULT_CHARSET.name()));
        userDao.save(user);
    }

    @Test
    public void should_save_success() {
        // when: get user by email
        User user = userDao.get("liangpengyv@fir.im");
        Assert.assertNotNull(user);
        Assert.assertEquals("liangpengyv", user.getUsername());

        // when: get user by username
        user = userDao.getByUsername("liangpengyv");
        Assert.assertNotNull(user);
        Assert.assertEquals("liangpengyv@fir.im", user.getEmail());
    }


    @Test
    public void should_delete_list_success() {
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));
        userDao.delete(ImmutableList.of("liangpengyv@fir.im"));
        Assert.assertNull(userDao.get("liangpengyv@fir.im"));
    }
}
