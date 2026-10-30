package com.library.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Validates the staff JWT on every /api request and passes the user to the services as
 * X-User-* headers. Client-supplied X-User-* headers are always dropped, and internal
 * service-to-service endpoints are not reachable from outside.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final List<String> PUBLIC = List.of("/api/auth/login");
    private static final List<String> INTERNAL = List.of(
            "/api/books/*/checkout", "/api/books/*/return", "/api/loans/*/reminded");
    private static final Set<String> USER_HEADERS = Set.of("x-user-id", "x-user-name", "x-user-email", "x-user-role");

    private final SecretKey key;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public JwtAuthFilter(@Value("${library.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (INTERNAL.stream().anyMatch(p -> matcher.match(p, path))) {
            error(response, HttpServletResponse.SC_FORBIDDEN, "Bu uç nokta yalnızca servisler arası kullanılır");
            return;
        }
        if (PUBLIC.contains(path)) {
            chain.doFilter(new UserRequest(request, Map.of()), response);
            return;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            error(response, HttpServletResponse.SC_UNAUTHORIZED, "Oturum açmanız gerekiyor");
            return;
        }
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(auth.substring(7)).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            error(response, HttpServletResponse.SC_UNAUTHORIZED, "Oturum süresi dolmuş veya geçersiz");
            return;
        }

        Map<String, String> user = new HashMap<>();
        user.put("x-user-id", claims.getSubject());
        user.put("x-user-email", String.valueOf(claims.get("email")));
        user.put("x-user-role", String.valueOf(claims.get("role")));
        // Header values must be ASCII; services URL-decode the name.
        user.put("x-user-name", URLEncoder.encode(String.valueOf(claims.get("name")), StandardCharsets.UTF_8));
        chain.doFilter(new UserRequest(request, user), response);
    }

    private static void error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }

    /** Request view with X-User-* headers replaced by the values taken from the token. */
    private static final class UserRequest extends HttpServletRequestWrapper {
        private final Map<String, String> user;

        UserRequest(HttpServletRequest request, Map<String, String> user) {
            super(request);
            this.user = user;
        }

        @Override
        public String getHeader(String name) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (USER_HEADERS.contains(lower)) return user.get(lower);
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (USER_HEADERS.contains(lower)) {
                String v = user.get(lower);
                return Collections.enumeration(v == null ? List.of() : List.of(v));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            for (Enumeration<String> e = super.getHeaderNames(); e.hasMoreElements(); ) {
                String n = e.nextElement();
                if (!USER_HEADERS.contains(n.toLowerCase(Locale.ROOT))) names.add(n);
            }
            names.addAll(user.keySet());
            return Collections.enumeration(names);
        }
    }
}
