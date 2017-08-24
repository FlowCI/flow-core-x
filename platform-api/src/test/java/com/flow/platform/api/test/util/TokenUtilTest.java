package com.flow.platform.api.test.util;

import com.flow.platform.api.util.TokenUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

/**
 * @author liangpengyv
 */
public class TokenUtilTest {

    @Test
    public void should_create_token_success() {
        String token = TokenUtil.createToken("liangpengyv@fir.im", 60 * 1000);
        Assert.assertNotNull(token);
    }

    @Test
    public void should_check_token_success() {
        String token = TokenUtil.createToken("liangpengyv@fir.im", 60 * 1000);
        String email = TokenUtil.checkToken(token);
        Assert.assertEquals("liangpengyv@fir.im", email);
    }
}
