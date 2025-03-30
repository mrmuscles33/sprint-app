package com.spring.application.configuration;

import com.spring.application.filter.JWTFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JWTFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Disable default security features
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.csrf(AbstractHttpConfigurer::disable);
        http.logout(AbstractHttpConfigurer::disable);
        http.cors(AbstractHttpConfigurer::disable);

        // Filters
        http.addFilterBefore(jwtFilter, BasicAuthenticationFilter.class);

        // Authorization
        http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
                // All requests to /auth are allowed
                .requestMatchers("/auth/**").permitAll()
                // All other requests are authenticated
                .anyRequest().authenticated()
        );

        return http.build();
    }
}
