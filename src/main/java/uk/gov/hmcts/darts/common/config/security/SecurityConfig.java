package uk.gov.hmcts.darts.common.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import uk.gov.hmcts.darts.authentication.component.UriProvider;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("!intTest")
public class SecurityConfig {

    private final UriProvider uriProvider;

    @Bean
    @Order(1)
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public SecurityFilterChain patternFilterChain(HttpSecurity http) throws Exception {
        applyCommonConfig(http)
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
                "/external-user/login-or-refresh",
                "/external-user/handle-oauth-code",
                "/"
            )
            .authorizeHttpRequests().anyRequest().permitAll();

        return http.build();
    }

    @Bean
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "squid:S4502"})
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        applyCommonConfig(http)
            .authorizeHttpRequests().anyRequest().authenticated()
            .and()
            .oauth2Login().loginPage(uriProvider.getAuthorizationUri().toString())
            .and()
            .oauth2ResourceServer().jwt();
        return http.build();
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private HttpSecurity applyCommonConfig(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .csrf().disable()
            .formLogin().disable()
            .logout().disable();
    }

}
