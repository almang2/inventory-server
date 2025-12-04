package com.almang.inventory.global.config.security;

import com.almang.inventory.global.security.jwt.JwtAuthEntryPoint;
import com.almang.inventory.global.security.jwt.JwtTokenProvider;
import com.almang.inventory.global.security.jwt.TokenAuthenticationFilter;
import com.almang.inventory.user.auth.service.RedisService;
import com.almang.inventory.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // HttpMethod import 추가
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RedisService redisService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    private static final String[] PUBLIC_APIS = {
            // Auth
            "/api/v1/auth/login",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/reissue",

            // Store admin
            "/api/v1/store/admin",
            "/api/v1/admin/store",

            // Actuator (헬스체크용)
            "/actuator/health",

            // Swagger / API docs
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs",
            "/swagger-resources/**",

            // H2 console
            "/h2-console/**",

            // 기타 공개 엔드포인트
            "/docs/**",
            "/health",
            "/error",
            "/favicon.ico",

            // Cafe24 OAuth
            "/api/v1/cafe24/oauth/**",
            "/api/v1/cafe24/orders/test",
            "/api/v1/cafe24/customerorders/test"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(c -> {
                })
                .csrf(AbstractHttpConfigurer::disable)
                .headers(h -> h.frameOptions(FrameOptionsConfig::sameOrigin)) // H2 콘솔 접근 시 iframe 사용 허용 (동일 출처만 허용하여 보안
                                                                              // 유지)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 공개 엔드포인트
                        .requestMatchers(PUBLIC_APIS).permitAll()
                        // 카페24 주문 수신 엔드포인트 (인증 제외)
                        .requestMatchers(HttpMethod.POST, "/api/v1/customer-orders").permitAll()
                        // 다른 엔드포인트는 인증 필요
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(jwtAuthEntryPoint))
                .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:3000", "https://almang.vercel.app")); // 프론트엔드 경로
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(redisService, jwtTokenProvider, userRepository);
    }
}
