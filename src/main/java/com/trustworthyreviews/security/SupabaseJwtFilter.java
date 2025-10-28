package com.trustworthyreviews.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class SupabaseJwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SupabaseJwtFilter.class);

    private final SupabaseJwtService supabaseJwtService;

    public SupabaseJwtFilter(SupabaseJwtService supabaseJwtService) {
        this.supabaseJwtService = supabaseJwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String bearerToken = resolveToken(request);
            if (StringUtils.hasText(bearerToken)) {
                Optional<Authentication> authentication = supabaseJwtService.authenticate(bearerToken);
                authentication.ifPresent(auth -> SecurityContextHolder.getContext().setAuthentication(auth));
            }
        } catch (Exception ex) {
            log.error("Unexpected error while processing Supabase JWT", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
