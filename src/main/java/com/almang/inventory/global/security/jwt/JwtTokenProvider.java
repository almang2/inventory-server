package com.almang.inventory.global.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final Key secretKey;
    private final int jwtExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.access-token.expiration-minutes}") int jwtExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpiration = jwtExpiration;
    }

    public String generateAccessToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TimeUnit.MINUTES.toMillis(jwtExpiration));

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
