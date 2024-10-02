package uk.gov.hmcts.darts.common.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.darts.authentication.config.AuthStrategySelector;
import uk.gov.hmcts.darts.authentication.config.DefaultAuthConfigurationPropertiesStrategy;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.internal.InternalAuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.internal.InternalAuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.io.IOException;
import java.util.Map;


@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("!intTest")
public class SecurityConfig {

    private final AuthStrategySelector locator;

    private final DefaultAuthConfigurationPropertiesStrategy fallbackConfiguration;
    private final ExternalAuthConfigurationProperties externalAuthConfigurationProperties;
    private final ExternalAuthProviderConfigurationProperties externalAuthProviderConfigurationProperties;
    private final InternalAuthConfigurationProperties internalAuthConfigurationProperties;
    private final InternalAuthProviderConfigurationProperties internalAuthProviderConfigurationProperties;
    private final UserIdentity userIdentity;
    private final JwtDecoder jwtDecoder;

    @Bean
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
                "/external-user/reset-password",
                "/internal-user/login-or-refresh",
                "/internal-user/handle-oauth-code",
                "/"
            )
            .authorizeHttpRequests().anyRequest().permitAll();

        return http.build();
    }

    @Bean
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "squid:S4502"})
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        applyCommonConfig(http)
            .addFilterBefore(new AuthorisationTokenExistenceFilter(), OAuth2LoginAuthenticationFilter.class)
            .addFilterAfter(new InactiveUserCheck(), SecurityContextHolderFilter.class)
            .authorizeHttpRequests().anyRequest().authenticated()
            .and()
            .oauth2ResourceServer().authenticationManagerResolver(jwtIssuerAuthenticationManagerResolver());

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

    private JwtIssuerAuthenticationManagerResolver jwtIssuerAuthenticationManagerResolver() {
        Map<String, AuthenticationManager> authenticationManagers = Map.ofEntries(
            createAuthenticationEntry(externalAuthConfigurationProperties.getIssuerUri(),
                externalAuthProviderConfigurationProperties.getJwkSetUri()),
            createAuthenticationEntry(internalAuthConfigurationProperties.getIssuerUri(),
                internalAuthProviderConfigurationProperties.getJwkSetUri())
        );
        return new JwtIssuerAuthenticationManagerResolver(authenticationManagers::get);
    }

    private Map.Entry<String, AuthenticationManager> createAuthenticationEntry(String issuer,
        String jwkSetUri) {
        var jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build();

        OAuth2TokenValidator<Jwt> jwtValidator = JwtValidators.createDefaultWithIssuer(issuer);
        jwtDecoder.setJwtValidator(jwtValidator);

        var authenticationProvider = new JwtAuthenticationProvider(jwtDecoder);

        return Map.entry(issuer, authenticationProvider::authenticate);
    }

    private final class AuthorisationTokenExistenceFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer")) {
                filterChain.doFilter(request, response);
                return;
            }

            response.sendRedirect(locator.locateAuthenticationConfiguration(req -> fallbackConfiguration).getLoginUri(null).toString());
        }
    }

    private final class InactiveUserCheck extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

            try {
                String authHeader = request.getHeader("Authorization");

                var jwtDecoder = NimbusJwtDecoder.withJwkSetUri(externalAuthProviderConfigurationProperties.getJwkSetUri())
                    .jwsAlgorithm(SignatureAlgorithm.RS256)
                    .build();

                OAuth2TokenValidator<Jwt> jwtValidator = JwtValidators.createDefaultWithIssuer(externalAuthConfigurationProperties.getIssuerUri());
                jwtDecoder.setJwtValidator(jwtValidator);
                Jwt jwt = jwtDecoder.decode(authHeader.replace("Bearer ", ""));

                UserAccountEntity userAccountEntity = userIdentity.getUserAccount(jwt);

                if (!userAccountEntity.isActive()) {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                } else {
                    filterChain.doFilter(request, response);
                }
            } catch (Exception exception) {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
            }
        }
    }
}