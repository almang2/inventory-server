package com.almang.inventory.global.security.jwt;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private static final String TOKEN_PREFIX = "Bearer ";

    // 화이트리스트
    private final List<String> whitelist = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/reissue",
            "/api/v1/store/admin",
            "/docs",
            "/health",
            "/h2-console",
            "/swagger-ui",
            "/v3/api-docs"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return whitelist.stream().anyMatch(path::equals) ||
                whitelist.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        request.removeAttribute("authErrorCode");

        String token = resolveToken(request);

        if (token == null || token.isBlank()) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        TokenStatus status = jwtTokenProvider.validateToken(token);

        log.debug("[AUTH] path={}, authHeader={}", request.getServletPath(), request.getHeader(HttpHeaders.AUTHORIZATION));
        log.debug("[AUTH] tokenStatus={}", status);
        if (status == TokenStatus.VALID) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

            GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
            List<GrantedAuthority> authorities = List.of(authority);
            CustomUserPrincipal principal =
                    new CustomUserPrincipal(user.getId(), user.getUsername(), authorities);

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        } else {
            SecurityContextHolder.clearContext();
            ErrorCode errorCode = (status == TokenStatus.EXPIRED)
                    ? ErrorCode.ACCESS_TOKEN_EXPIRED
                    : ErrorCode.ACCESS_TOKEN_INVALID;
            request.setAttribute("authErrorCode", errorCode);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (bearer != null && bearer.startsWith(TOKEN_PREFIX)) {
            return bearer.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
