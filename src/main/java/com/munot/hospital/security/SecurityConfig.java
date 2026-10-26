package com.munot.hospital.security;

import com.munot.hospital.config.AppProperties;
import com.munot.hospital.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final AppProperties props;

    public SecurityConfig(AppProperties props) { this.props = props; }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt, UserRepository users) throws Exception {
        http.csrf(c -> c.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/public/**", "/actuator/health",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> json(res, 401, "Authentication required"))
                .accessDeniedHandler((req, res, ex) -> json(res, 403, "Access denied")))
            .headers(h -> h.frameOptions(f -> f.deny()).contentTypeOptions(Customizer.withDefaults()))
            .addFilterBefore(new JwtAuthFilter(jwt, users), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void json(HttpServletResponse res, int status, String msg) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("{\"status\":" + status + ",\"message\":\"" + msg + "\"}");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        List<String> origins = Arrays.stream(props.corsAllowedOrigins().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        c.setAllowedOrigins(origins);
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        c.setExposedHeaders(List.of("Content-Disposition"));
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c);
        return src;
    }
}
