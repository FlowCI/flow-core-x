/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.test.auth;

import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.user.domain.User;
import com.flowci.core.auth.helper.JwtHelper;
import com.flowci.domain.ObjectWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JwtHelperTest {

    private final User user  = new User("test@flow.ci", "12345", User.Role.Admin);

    private ObjectWrapper<String> token = new ObjectWrapper<>();

    @Before
    public void createToken() {
        user.setRole(User.Role.Admin);

        token.setValue(JwtHelper.create(user, 60));
        Assert.assertNotNull(token.getValue());
    }

    @Test
    public void should_decode_token_and_get_user_id() {
        String email = JwtHelper.decode(token.getValue());
        Assert.assertEquals(user.getEmail(), email);
    }

    @Test
    public void should_verify_token() {
        boolean verify = JwtHelper.verify(token.getValue(), user, false);
        Assert.assertTrue(verify);
    }

    @Test
    public void should_fail_if_pw_changed() {
        user.setPasswordOnMd5("22345");
        boolean verify = JwtHelper.verify(token.getValue(), user, false);
        Assert.assertFalse(verify);
    }

    @Test
    public void should_fail_if_token_expired() {
        // init: set expired seconds to 1
        token.setValue(JwtHelper.create(user, 1));

        // when:
        ThreadHelper.sleep(3000);
        boolean verify = JwtHelper.verify(token.getValue(), user, true);

        // then: should be fail
        Assert.assertFalse(verify);
    }
}
