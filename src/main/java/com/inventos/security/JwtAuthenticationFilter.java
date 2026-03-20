package com.inventos.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil                jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenBlacklistService  blacklistService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   UserDetailsServiceImpl userDetailsService,
                                   TokenBlacklistService blacklistService) {
        this.jwtUtil           = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.blacklistService  = blacklistService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res); return;
        }
        String token = header.substring(7);
        String username;
        try { username = jwtUtil.extractUsername(token); }
        catch (Exception e) { chain.doFilter(req, res); return; }

        if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
            String jti = jwtUtil.extractJti(token);
            if (blacklistService.isRevoked(jti)) { chain.doFilter(req, res); return; }
            UserDetails user = userDetailsService.loadUserByUsername(username);
            if (jwtUtil.isTokenValid(token, user)) {
                List<SimpleGrantedAuthority> auths = jwtUtil.extractRoles(token).stream()
                    .map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                var authToken = new UsernamePasswordAuthenticationToken(user, null, auths);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        chain.doFilter(req, res);
    }
}
