package com.flow.platform.api.test.controller;

import com.flow.platform.api.dao.UserDao;
import com.flow.platform.api.domain.User;
import com.flow.platform.api.service.UserService;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author liangpengyv
 */
public class UserControllerTest extends TestBase {

    @Autowired
    private UserService userService;

    @Autowired
    private UserDao userDao;

    private User user;

    @Before
    public void beforeTest() {
        user = new User();
        user.setEmail("liangpengyv@fir.im");
        user.setUserName("liangpengyv");
        user.setPassword("liangpengyv");
        user.setRoleId("developer");
        userService.register(user);
        user.setPassword("liangpengyv");
    }

    @Test
    public void should_login_success() throws Throwable {
        String requestContent;
        String responseContent;
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder;
        MvcResult mvcResult;

        requestContent = "{ \"email\" : \"liangpengyv@fir.im\", \"password\" : \"liangpengyv\" }";
        mockHttpServletRequestBuilder = post("/user/login").contentType(MediaType.APPLICATION_JSON).content(requestContent);
        mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();
        responseContent = mvcResult.getResponse().getContentAsString();
        Assert.assertTrue(responseContent.length() > 20);

        requestContent = "{ \"userName\" : \"liangpengyv\", \"password\" : \"liangpengyv\" }";
        mockHttpServletRequestBuilder = post("/user/login").contentType(MediaType.APPLICATION_JSON).content(requestContent);
        mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();
        responseContent = mvcResult.getResponse().getContentAsString();
        Assert.assertTrue(responseContent.length() > 20);
    }

    @Test
    public void should_register_success() throws Throwable {
        String requestContent;
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder;

        requestContent = "{ \"email\" : \"test1@fir.im\", \"userName\" : \"test1\", \"password\" : \"test1\", \"roleId\" : \"developer\" }";
        mockHttpServletRequestBuilder = post("/user/register").contentType(MediaType.APPLICATION_JSON).content(requestContent);
        mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();
        Assert.assertNotNull(userDao.get("test1@fir.im"));
    }

    @Test
    public void should_delete_user_success() throws Throwable {
        String requestContent;
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder;

        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));

        requestContent = "[ \"liangpengyv@fir.im\" ]";
        mockHttpServletRequestBuilder = post("/user/delete").contentType(MediaType.APPLICATION_JSON).content(requestContent);
        mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();
        Assert.assertNull(userDao.get("liangpengyv@fir.im"));
    }

    @Test
    public void should_switch_role_success() throws Throwable {
        String requestContent;
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder;
        MvcResult mvcResult;

        Assert.assertEquals("developer", userDao.get("liangpengyv@fir.im").getRoleId());

        requestContent = "{ \"emailList\" : [ \"liangpengyv@fir.im\" ], \"switchTo\" : \"admin\" }";
        mockHttpServletRequestBuilder = post("/user/switch_role").contentType(MediaType.APPLICATION_JSON).content(requestContent);
        mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();
        mvcResult.getResponse().getContentAsString();
        Assert.assertEquals("admin", userDao.get("liangpengyv@fir.im").getRoleId());
    }
}
