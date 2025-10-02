package QuizApp.example.QuizApp.Utility;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;


@Component

public class JwtUtil {
    @Value("${jwt.secret}")
    private String jwtSecret;

    private Key getSigningKey(){
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, String role, String email){
        return Jwts.builder()
                .setSubject(username)
                .setAudience(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+3600000))
                .signWith(getSigningKey())
                .compact();
    }
    public String extractUsername(String token){
        return extractClaim(token,Claims::getSubject);
    }
    public boolean validateToken(String token, UserDetails userdetails){
        final String extractedusername = extractUsername(token);
        return (extractedusername.equals(userdetails.getUsername())&&!isTokenExpired(token));
    }
    public boolean isTokenExpired(String token){
        return extractClaim(token,Claims::getExpiration).before(new Date());
    }
    public String extractEmail(String token){
        return extractClaim(token, Claims::getAudience);
    }
    private <T> T extractClaim(String token, ClaimsExtractor<T> claimsExtractor){
        Claims claim = extractAllClaims(token);
        return claimsExtractor.extract(claim);
    }
    private Claims extractAllClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    @FunctionalInterface
    private interface ClaimsExtractor<T> {
        T extract(Claims claim);
    }
}

