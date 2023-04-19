package uk.gov.hmcts.darts.common.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@ConfigurationProperties(prefix = "spring.security")
public class SecurityConfig {

    @Bean
    @Order(1)
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public SecurityFilterChain patternFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/swagger-resources/**",
                "/v3/**",
                "/favicon.ico",
                "/health/**",
                "/mappings",
                "/info",
                "/metrics",
                "/metrics/**",
                "/"
            )
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .permitAll()
            );

        return http.build();
    }

    @Bean
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "squid:S4502"})
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .httpBasic()
            .and()
            .formLogin().disable()
            .logout().disable()
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }

}
