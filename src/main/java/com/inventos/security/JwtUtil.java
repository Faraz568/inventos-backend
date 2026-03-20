package com.inventos.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component @Slf4j
public class JwtUtil {
    @Value("${app.jwt.secret}") private String secret;
    @Value("${app.jwt.expiration-ms}") private long expirationMs;

    private SecretKey key() { return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)); }

    public String generateToken(UserDetails user) {
        List<String> roles = user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        return Jwts.builder()
            .claims(Map.of("roles", roles))
            .subject(user.getUsername())
            .id(UUID.randomUUID().toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(key(), Jwts.SIG.HS256).compact();
    }

    public String extractUsername(String token)  { return extractClaim(token, Claims::getSubject); }
    public String extractJti(String token)        { return extractClaim(token, Claims::getId); }
    public Date   extractExpiration(String token) { return extractClaim(token, Claims::getExpiration); }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object r = extractAllClaims(token).get("roles");
        return r instanceof List<?> l ? l.stream().map(Object::toString).collect(Collectors.toList()) : List.of();
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }
    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }
    public boolean isTokenValid(String token, UserDetails user) {
        try { return extractUsername(token).equals(user.getUsername()) && !isExpired(token); }
        catch (JwtException | IllegalArgumentException e) { log.warn("Invalid JWT: {}", e.getMessage()); return false; }
    }
    public boolean isExpired(String token)  { return extractExpiration(token).before(new Date()); }
    public long    getExpirationSeconds()   { return expirationMs / 1000; }
}
