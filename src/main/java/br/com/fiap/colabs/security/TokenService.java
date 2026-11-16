package br.com.fiap.colabs.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class TokenService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public TokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            try {
                keyBytes = java.security.MessageDigest.getInstance("SHA-256")
                        .digest(secret.getBytes(StandardCharsets.UTF_8));
            } catch (java.security.NoSuchAlgorithmException ex) {
                keyBytes = java.util.Arrays.copyOf(keyBytes, 32);
            }
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(AccountUserDetails user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(user.getUsername())
                .claim("accountId", user.getAccountId())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry);

        if (user.getCitizenId() != null) {
            builder.claim("citizenId", user.getCitizenId());
        }
        if (user.getCollectionPointId() != null) {
            builder.claim("collectionPointId", user.getCollectionPointId());
        }

        return builder.signWith(signingKey).compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String expectedUsername) {
        Claims claims = parseClaims(token);
        return claims.getSubject().equals(expectedUsername) && claims.getExpiration().after(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}