package com.attendance.facesystem.config;

import com.attendance.facesystem.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Deliberately "simple" security setup as requested:
 *  - Stateless JWT auth (no sessions/cookies to manage)
 *  - Auth endpoints (signup/login/forgot/reset) are public
 *  - Face-check + attendance-marking stay public too, because the live kiosk
 *    camera recognizes WHOEVER stands in front of it - it isn't tied to a
 *    logged-in browser session
 *  - Anything that changes a specific account (completing profile,
 *    registering a face) requires a valid JWT
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Please log in to continue.\",\"data\":null}");
            }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
      				"/",
        			"/health",
        			"/actuator/health",

        			"/api/auth/**",

        			"/api/face/check",
        			"/api/face/liveness-check",

        			"/api/attendance/mark",
        			"/api/attendance/all",
        			"/api/attendance/date",
       				"/api/attendance/user/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}





