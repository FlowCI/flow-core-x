/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.api.test.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.domain.EmailSetting;
import com.flow.platform.api.domain.response.SmtpAuthResponse;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.domain.Jsonable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yh@firim
 */
public class MessageControllerTest extends TestBase {

    @Test
    public void should_create_email_setting_success() {

        EmailSetting emailSetting = new EmailSetting("smtp.163.com", 465, "xxxx@163.com");

        MockHttpServletRequestBuilder content = post("/message/emailSetting")
            .contentType(MediaType.APPLICATION_JSON)
            .content(emailSetting.toJson());
        try {
            MvcResult mvcResult = this.mockMvc.perform(content)
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

            String response = mvcResult.getResponse().getContentAsString();
            EmailSetting setting = Jsonable.GSON_CONFIG.fromJson(response, EmailSetting.class);

            Assert.assertEquals(emailSetting.getSmtpUrl(), setting.getSmtpUrl());
            Assert.assertEquals(emailSetting.getSmtpPort(), setting.getSmtpPort());
            Assert.assertEquals(emailSetting.getSender(), setting.getSender());
            Assert.assertEquals(emailSetting.getUsername(), setting.getUsername());
            Assert.assertEquals(emailSetting.getPassword(), setting.getPassword());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void should_update_success() {
        EmailSetting emailSetting = new EmailSetting("smtp.163.com", 465, "xxxx@163.com");

        MockHttpServletRequestBuilder content = post("/message/emailSetting")
            .contentType(MediaType.APPLICATION_JSON)
            .content(emailSetting.toJson());
        try {
            MvcResult mvcResult = this.mockMvc.perform(content)
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

            emailSetting.setPassword("123456");
            content = patch("/message/emailSetting")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emailSetting.toJson());

            mvcResult = this.mockMvc.perform(content)
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

            String response = mvcResult.getResponse().getContentAsString();
            EmailSetting setting = Jsonable.GSON_CONFIG.fromJson(response, EmailSetting.class);

            Assert.assertEquals(emailSetting.getPassword(), setting.getPassword());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void should_auth_failure() {
        EmailSetting emailSetting = new EmailSetting("smtp.163.com", 465, "xxxx@163.com");

        MockHttpServletRequestBuilder content = post("/message/emailSetting/auth")
            .contentType(MediaType.APPLICATION_JSON)
            .content(emailSetting.toJson());
        try {
            MvcResult mvcResult = this.mockMvc.perform(content)
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

            String response = mvcResult.getResponse().getContentAsString();
            SmtpAuthResponse smtpAuthResponse = Jsonable.GSON_CONFIG.fromJson(response, SmtpAuthResponse.class);

            Assert.assertEquals(false, smtpAuthResponse.getAuth());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
