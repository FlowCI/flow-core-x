package com.flow.platform.api.test.util;

import com.flow.platform.api.security.token.JwtTokenGenerator;
import com.flow.platform.api.security.token.TokenGenerator;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author liangpengyv
 */
public class TokenUtilTest {

    private TokenGenerator generator = new JwtTokenGenerator("MY_SECRET_KEY");

    @Test
    public void should_create_token_success() {
        String token = generator.create("liangpengyv@fir.im", 60 * 1000);
        Assert.assertNotNull(token);
    }

    @Test
    public void should_check_token_success() {
        String token = generator.create("liangpengyv@fir.im", 60 * 1000);
        String email = generator.extract(token);
        Assert.assertEquals("liangpengyv@fir.im", email);
    }
}
