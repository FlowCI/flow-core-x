package com.flow.platform.cc.test;

import com.flow.platform.cc.config.WebConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {WebConfig.class})
public class WelcomeControllerTest {

    @Autowired
    private WebApplicationContext webAppContext;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();
    }

    @Test
    public void should_get_ok_on_index() throws Exception {
        this.mockMvc.perform(get("/index"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

}