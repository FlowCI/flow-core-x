package com.flow.platform.api.util;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.core.exception.IllegalParameterException;
import io.jsonwebtoken.*;

import java.io.UnsupportedEncodingException;
import java.util.Date;

/**
 * @author liangpengyv
 */
public class TokenUtil {

    private static final String secret = "MY_SECRET_KEY";

    /**
     * Through the email of user to create a token.
     * At the same time, you need provide a expiration duration, It's in millisecond.
     *
     * @param email
     * @param expirationDuration
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String createToken(String email, long expirationDuration) {
        try {
            String compactJws = Jwts.builder()
                    .signWith(SignatureAlgorithm.HS256, secret.getBytes(AppConfig.DEFAULT_CHARSET.name()))
                    .setSubject(email)
                    .setExpiration(new Date(new Date().getTime() + expirationDuration))
                    .compact();
            return compactJws;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Through the string of token to get the email of user.
     * If the token is expired, will throw a ExpiredJwtException
     *
     * @param tokenStr
     * @return
     */
    public static String checkToken(String tokenStr) {
        String email;
        String errMsg = "Illegal token parameter: ";
        try {
            Jws<Claims> jws = Jwts.parser()
                    .setSigningKey(secret.getBytes(AppConfig.DEFAULT_CHARSET.name()))
                    .parseClaimsJws(tokenStr);

            email = jws.getBody().getSubject();
            return email;
        } catch (ExpiredJwtException e) {
            // expired token
            throw new IllegalParameterException(errMsg + "expired token");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
