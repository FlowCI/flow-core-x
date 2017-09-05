package com.flow.platform.api.security.token;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.exception.SecurityTokenException;
import com.flow.platform.api.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.UnsupportedEncodingException;
import java.util.Date;

/**
 * @author liangpengyv
 */
public class JwtTokenGenerator implements TokenGenerator {

    private final String secret;

    public JwtTokenGenerator(String secret) {
        this.secret = secret;
    }

    /**
     * Through the email of user to create a token.
     * At the same time, you need provide a expiration duration, It's in millisecond.
     */
    @Override
    public String create(String email, long duration) {
        try {
            return Jwts.builder()
                .signWith(SignatureAlgorithm.HS256, secret.getBytes(AppConfig.DEFAULT_CHARSET.name()))
                .setSubject(email)
                .setExpiration(new Date(new Date().getTime() + duration))
                .compact();
        } catch (UnsupportedEncodingException e) {
            throw new SecurityTokenException(e.getMessage());
        }
    }

    /**
     * Through the string of token to get the email of user.
     * If the token is expired, will throw a ExpiredJwtException
     */
    @Override
    public String extract(String tokenStr) {
        try {
            Jws<Claims> jws = Jwts.parser()
                .setSigningKey(secret.getBytes(AppConfig.DEFAULT_CHARSET.name()))
                .parseClaimsJws(tokenStr);

            return jws.getBody().getSubject();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch (UnsupportedEncodingException e) {
            throw new SecurityTokenException(e.getMessage());
        }
    }
}
