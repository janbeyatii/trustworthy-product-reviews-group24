package com.trustworthyreviews.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
public class SupabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(SupabaseConfig.class);

    @Bean
    public SupabaseProperties supabaseProperties(Environment environment) {
        SupabaseProperties properties = new SupabaseProperties(
                environment.getProperty("SUPABASE_URL"),
                environment.getProperty("SUPABASE_ANON_KEY"),
                environment.getProperty("SUPABASE_SERVICE_ROLE"),
                environment.getProperty("SUPABASE_JWT_SECRET")
        );

        if (!StringUtils.hasText(properties.getUrl()) || !StringUtils.hasText(properties.getAnonKey())) {
            log.warn("Supabase URL or anonymous key is not configured. Frontend authentication will not function correctly until these are provided.");
        }

        if (!properties.hasJwtSecret()) {
            log.info("SUPABASE_JWT_SECRET is not configured. Protected API routes will allow unauthenticated access unless additional security is configured.");
        }

        return properties;
    }

    public static class SupabaseProperties {
        private final String url;
        private final String anonKey;
        private final String serviceRoleKey;
        private final String jwtSecret;

        public SupabaseProperties(String url, String anonKey, String serviceRoleKey, String jwtSecret) {
            this.url = url;
            this.anonKey = anonKey;
            this.serviceRoleKey = serviceRoleKey;
            this.jwtSecret = jwtSecret;
        }

        public String getUrl() {
            return url;
        }

        public String getAnonKey() {
            return anonKey;
        }

        public String getServiceRoleKey() {
            return serviceRoleKey;
        }

        public String getJwtSecret() {
            return jwtSecret;
        }

        public boolean hasJwtSecret() {
            return StringUtils.hasText(jwtSecret);
        }
    }
}

