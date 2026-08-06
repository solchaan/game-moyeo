package com.gamemoyeo.common.config;

import com.gamemoyeo.auth.adapter.in.web.SocialLoginSuccessHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
    SecurityFilterChain securedApi(
        HttpSecurity http,
        SocialLoginSuccessHandler socialLoginSuccessHandler
    ) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**", "/api/v1/auth/token").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/games/**", "/api/v1/meetups/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt ->
                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .oauth2Login(login -> login.successHandler(socialLoginSuccessHandler))
            .build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            java.util.List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                return java.util.List.of();
            }
            return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        });
        return converter;
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "false", matchIfMissing = true)
    SecurityFilterChain localApi(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
            .build();
    }
}
