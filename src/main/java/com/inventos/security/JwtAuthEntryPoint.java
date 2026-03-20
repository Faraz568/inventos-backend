package com.inventos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component @Slf4j
public class JwtAuthEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        log.warn("Unauthorized: {}", req.getRequestURI());
        write(res, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
    }
    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        log.warn("Forbidden: {}", req.getRequestURI());
        write(res, HttpServletResponse.SC_FORBIDDEN, "Access denied");
    }
    private void write(HttpServletResponse res, int status, String msg) throws IOException {
        res.setStatus(status); res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        MAPPER.writeValue(res.getOutputStream(),
            Map.of("success", false, "message", msg, "status", status, "timestamp", LocalDateTime.now().toString()));
    }
}
