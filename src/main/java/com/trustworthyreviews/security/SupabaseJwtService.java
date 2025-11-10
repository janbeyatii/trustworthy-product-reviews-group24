package com.trustworthyreviews.security;

import com.trustworthyreviews.config.SupabaseConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256;

@Service
public class SupabaseJwtService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseJwtService.class);

    private final SupabaseConfig.SupabaseProperties properties;
    private JwtDecoder jwtDecoder;

    public SupabaseJwtService(SupabaseConfig.SupabaseProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initializeDecoder() {
        if (properties == null) {
            log.warn("Supabase properties not configured. API requests will not be authenticated.");
            return;
        }

        if (properties.hasJwtSecret()) {
            SecretKey secretKey = new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            this.jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(HS256).build();
            log.info("Supabase JWT decoder initialised with provided secret.");
            return;
        }

        if (StringUtils.hasText(properties.getUrl())) {
            String jwkSetUri = properties.getUrl().replaceAll("/+$", "") + "/auth/v1/keys";
            this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
            log.info("Supabase JWT decoder initialised using remote JWKS: {}", jwkSetUri);
            return;
        }

        log.warn("Supabase JWT secret or JWKS endpoint not configured. API requests will not be authenticated.");
    }

    public Optional<Authentication> authenticate(String token) {
        if (!StringUtils.hasText(token) || jwtDecoder == null) {
            return Optional.empty();
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            String subject = jwt.getSubject();
            Map<String, Object> claims = jwt.getClaims();
            String email = (String) claims.getOrDefault("email", subject);
            SupabaseUser user = new SupabaseUser(subject, email, claims);
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(user, token, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            return Optional.of(authenticationToken);
        } catch (JwtException ex) {
            log.warn("Failed to decode Supabase JWT: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
