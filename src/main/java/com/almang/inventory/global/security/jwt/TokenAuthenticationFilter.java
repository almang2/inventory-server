package com.almang.inventory.global.security.jwt;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.user.auth.service.RedisService;
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

    private final RedisService redisService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private static final String TOKEN_PREFIX = "Bearer ";

    private final List<String> whitelist = List.of(
            // Auth
            "/api/v1/auth/login",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/reissue",

            // Store admin
            "/api/v1/store/admin",
            "/api/v1/admin/store",

            // Cafe24 OAuth (모든 하위 경로 포함)
            "/api/v1/oauth/cafe24",

            // Actuator (헬스체크용)
            "/actuator/health",

            // Swagger / API docs
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",

            // H2 console
            "/h2-console",

            // 기타 공개 엔드포인트
            "/docs",
            "/health",
            "/error",
            "/favicon.ico"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        boolean shouldSkip = whitelist.stream().anyMatch(whitePath -> {
            // 정확히 일치하거나, whitePath로 시작하는 경로인지 확인
            boolean matches = path.equals(whitePath) || path.startsWith(whitePath + "/");
            if (matches) {
                log.debug("[AUTH] Filter skipped for path: {}", path);
            }
            return matches;
        });
        return shouldSkip;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        
        // 추가 안전장치: whitelist에 있는 경로는 필터를 건너뛰기
        boolean isWhitelisted = whitelist.stream().anyMatch(whitePath -> 
                path.equals(whitePath) || path.startsWith(whitePath + "/"));
        
        if (isWhitelisted) {
            log.debug("[AUTH] Whitelisted path, skipping filter: {}", path);
            filterChain.doFilter(request, response);
            return;
        }
        
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
            if (redisService.isAccessTokenBlacklisted(token)) {
                log.warn("블랙리스트에 등록된 액세스 토큰입니다.");
                SecurityContextHolder.clearContext();
                request.setAttribute("authErrorCode", ErrorCode.ACCESS_TOKEN_INVALID);
            } else {
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
            }
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
