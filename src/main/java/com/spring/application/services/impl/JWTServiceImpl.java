package com.spring.application.services.impl;

import com.spring.application.services.interfaces.JWTService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

@Service
@Log4j2
public class JWTServiceImpl implements JWTService {

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expire}")
    private long expire;
    @Value("${jwt.issuer}")
    private String issuer;

    @Override
    public String createToken(String username) {
        return this.createToken(username, Collections.emptyMap());
    }

    @Override
    public String createToken(String username, Map<String, ?> params) {
        return Jwts.builder()
                .issuer(this.getClass().getCanonicalName())
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + this.expire))
                .claims(params)
                .signWith(this.getKey())
                .compact();
    }

    @Override
    public String getUsername(String token) {
        return Jwts.parser()
                .verifyWith(this.getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    @Override
    public Map<String, Object> getParams(String token) {
        return Jwts.parser()
                .verifyWith(this.getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(this.getKey()).build().parse(token);
            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secret));
    }
}
