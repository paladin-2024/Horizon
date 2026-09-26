package com.horizon.auth;

import com.horizon.common.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Turns a valid, unexpired hz_access JWT into an {@link AuthenticatedUser} principal. An expired or
 * invalid one is ignored (the request stays unauthenticated and gets 401 from the chain), never
 * rejected here: /auth/refresh and /auth/logout must keep working with a stale access cookie. This
 * filter writes no response body, so it needs no ProblemWriter.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            TokenCookies.read(request, TokenCookies.ACCESS_COOKIE)
                    .flatMap(jwtService::verifyAccessToken)
                    .ifPresent(userId -> SecurityContextHolder.getContext().setAuthentication(
                            UsernamePasswordAuthenticationToken.authenticated(
                                    new AuthenticatedUser(userId), null, List.of())));
        }
        filterChain.doFilter(request, response);
    }
}
