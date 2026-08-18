package com.pharmagarde.backend.infrastructure.security;

import com.pharmagarde.backend.domain.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Composant responsable de la génération et de la validation des tokens JWT.
 * Le token embarque : l'email (subject), l'id utilisateur, le rôle,
 * le tenantId (portée pharmacie) et le zoneId (portée superviseur de zone).
 */
@Component
public class JwtTokenProvider {

    // Clé secrète utilisée pour signer/vérifier les tokens (HMAC-SHA256)
    private final SecretKey key;

    // Durée de validité d'un token, en millisecondes (ex: 86400000 = 24h)
    private final long expirationMs;

    // Injection des valeurs définies dans application.properties
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // Conversion de la chaîne secrète en clé cryptographique utilisable par jjwt
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Génère un token JWT signé pour un utilisateur donné.
     * Les claims custom (userId, role, tenantId, zoneId) permettent au filtre
     * de sécurité de connaître la portée de l'utilisateur sans requêter la BDD
     * à chaque appel.
     */
public String generateToken(Long userId, String email, Role role, Long tenantId, Long zoneId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)                  // identifiant principal du token
                .claim("userId", userId)
                .claim("role", role.name())      // stocké en String (ex: "ADMIN")
                .claim("tenantId", tenantId)      // peut être null (ex: SUPERVISEUR général)
                .claim("zoneId", zoneId)          // peut être null (ex: ADMIN, SUPERVISEUR général)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)                    // algorithme déduit automatiquement de la clé (HS256)
                .compact();
    }

    /**
     * Décode et vérifie la signature du token, puis retourne son contenu (claims).
     * Lève une exception si le token est invalide, expiré ou altéré.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Vérifie qu'un token est valide (signature correcte + non expiré),
     * sans lever d'exception. Utilisé par le filtre de sécurité.
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            // Token expiré, signature invalide, format incorrect, etc.
            return false;
        }
    }

    // --- Accesseurs pratiques pour extraire une info précise du token ---

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    public Long getTenantIdFromToken(String token) {
        return parseClaims(token).get("tenantId", Long.class);
    }

    public Long getZoneIdFromToken(String token) {
        return parseClaims(token).get("zoneId", Long.class);
    }
}