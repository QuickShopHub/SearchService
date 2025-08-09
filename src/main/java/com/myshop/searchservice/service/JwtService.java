package com.myshop.searchservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.security.PublicKey;

@Service
public class JwtService {


    private final PublicKey publicKey;

    public JwtService(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public Claims validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("JWT token is missing");
        }

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            // Токен просрочен
            throw ex;
        } catch (JwtException | IllegalArgumentException ex) {
            // Токен некорректный, неверный, подпись не совпадает и т.п.
            throw new RuntimeException("Invalid JWT token", ex);
        }
    }
}