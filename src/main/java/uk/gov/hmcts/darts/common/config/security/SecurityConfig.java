package uk.gov.hmcts.darts.common.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTParser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.darts.authentication.component.DartsJwt;
import uk.gov.hmcts.darts.authentication.config.AuthStrategySelector;
import uk.gov.hmcts.darts.authentication.config.DefaultAuthConfigurationPropertiesStrategy;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.internal.InternalAuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.internal.InternalAuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.marketplace.CpMarketplaceAuthConfigurationProperties;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiTrait;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("!intTest || tokenSecurityTest")
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class SecurityConfig {

    private final AuthStrategySelector locator;

    private final DefaultAuthConfigurationPropertiesStrategy fallbackConfiguration;
    private final ExternalAuthConfigurationProperties externalAuthConfigurationProperties;
    private final ExternalAuthProviderConfigurationProperties externalAuthProviderConfigurationProperties;
    private final InternalAuthConfigurationProperties internalAuthConfigurationProperties;
    private final InternalAuthProviderConfigurationProperties internalAuthProviderConfigurationProperties;
    private final CpMarketplaceAuthConfigurationProperties cpMarketplaceAuthConfigurationProperties;
    private final UserIdentity userIdentity;
    private final JwtDecoder jwtDecoder;
    private static final String TOKEN_BEARER_PREFIX = "Bearer";

    private final Map<String, NimbusJwtDecoder> decoderMap = new HashMap<>();

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
                "/external-user/refresh-access-token",
                "/external-user/reset-password",
                "/internal-user/login-or-refresh",
                "/internal-user/handle-oauth-code",
                "/internal-user/refresh-access-token",
                "/"
            )
            .authorizeHttpRequests().anyRequest().permitAll();

        return http.build();
    }

    @Bean
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "squid:S4502"})
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        applyCommonConfig(http)
            .addFilterBefore(new AuthorisationTokenExistenceFilter(), OAuth2LoginAuthenticationFilter.class)
            .addFilterAfter(new InactiveUserAuthorisationCheck(mapper), OAuth2LoginAuthenticationFilter.class)
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
        Map<String, AuthenticationManager> authenticationManagers = new HashMap<>();
        authenticationManagers.put(externalAuthConfigurationProperties.getIssuerUri(),
                                   createAuthenticationManager(externalAuthConfigurationProperties.getIssuerUri()));
        authenticationManagers.put(internalAuthConfigurationProperties.getIssuerUri(),
                                   createAuthenticationManager(internalAuthConfigurationProperties.getIssuerUri()));

        if (cpMarketplaceAuthConfigurationProperties.isEnabled()) {
            validateCpMarketplaceConfiguration();
            authenticationManagers.put(cpMarketplaceAuthConfigurationProperties.getIssuerUri(),
                                       createAuthenticationManager(cpMarketplaceAuthConfigurationProperties.getIssuerUri()));
        }

        return new JwtIssuerAuthenticationManagerResolver(authenticationManagers::get);
    }

    private AuthenticationManager createAuthenticationManager(String issuer) {
        var jwtDecoder = getNimbusJwtDecoder(issuer);

        //Use a custom JWT decoder so that we can add the user id to the JWT without modifying the underlying JWT
        //This is required for user auditing, as the audit event will need to know the user id but can not call the database to retrieve it
        //As such by wrapping the JWT we can persist the logged in user id without changing the root authentication
        var dartsJwtDecoder = new JwtDecoder() {
            @Override
            public Jwt decode(String token) {
                Jwt jwt = jwtDecoder.decode(token);
                Integer userId = userIdentity.getUserAccountOptional(jwt)
                    .map(UserAccountEntity::getId)
                    .orElse(null);
                return new DartsJwt(jwt, userId);
            }
        };

        OAuth2TokenValidator<Jwt> jwtValidator = createJwtValidator(issuer);
        jwtDecoder.setJwtValidator(jwtValidator);
        var authenticationProvider = new JwtAuthenticationProvider(dartsJwtDecoder);
        return authenticationProvider::authenticate;
    }

    private OAuth2TokenValidator<Jwt> createJwtValidator(String issuer) {
        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(issuer);

        if (!isCpMarketplaceIssuer(issuer)) {
            return defaultValidator;
        }

        return new DelegatingOAuth2TokenValidator<>(
            defaultValidator,
            jwt -> jwt.getAudience().contains(cpMarketplaceAuthConfigurationProperties.getAudience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The aud claim is not valid", null)),
            requiredClaimValidator(cpMarketplaceAuthConfigurationProperties.getIdentityClaim(),
                                   cpMarketplaceAuthConfigurationProperties.getExpectedIdentity()),
            requiredClaimValidator(cpMarketplaceAuthConfigurationProperties.getRoleClaim(),
                                   cpMarketplaceAuthConfigurationProperties.getExpectedRole())
        );
    }

    private OAuth2TokenValidator<Jwt> requiredClaimValidator(String claimName, String expectedValue) {
        return jwt -> claimContainsExpectedValue(jwt.getClaim(claimName), expectedValue)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The " + claimName + " claim is not valid", null));
    }

    private boolean claimContainsExpectedValue(Object claimValue, String expectedValue) {
        if (claimValue instanceof String claimString) {
            return expectedValue.equals(claimString);
        }
        if (claimValue instanceof Collection<?> claimCollection) {
            return claimCollection.contains(expectedValue);
        }
        return false;
    }

    private void validateCpMarketplaceConfiguration() {
        if (StringUtils.isAnyBlank(
            cpMarketplaceAuthConfigurationProperties.getIssuerUri(),
            cpMarketplaceAuthConfigurationProperties.getJwkSetUri(),
            cpMarketplaceAuthConfigurationProperties.getAudience(),
            cpMarketplaceAuthConfigurationProperties.getIdentityClaim(),
            cpMarketplaceAuthConfigurationProperties.getExpectedIdentity(),
            cpMarketplaceAuthConfigurationProperties.getRoleClaim(),
            cpMarketplaceAuthConfigurationProperties.getExpectedRole(),
            cpMarketplaceAuthConfigurationProperties.getServiceAccountEmail()
        )) {
            throw new IllegalStateException("CP Marketplace authentication is enabled but required configuration is missing");
        }
    }

    private final class AuthorisationTokenExistenceFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith(TOKEN_BEARER_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }

            response.sendRedirect(locator.locateAuthenticationConfiguration(req -> fallbackConfiguration).getLoginUri(null).toString());
        }
    }

    @RequiredArgsConstructor
    private final class InactiveUserAuthorisationCheck extends OncePerRequestFilter {

        private final ObjectMapper mapper;

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
            Jwt jwt = getJwtFromRequest(request);
            // if we can't determine the jwt then proxy and let the nimbus library handle the 401
            if (jwt == null) {
                filterChain.doFilter(request, response);
            } else {
                try {
                    userIdentity.getUserAccount(jwt);
                    filterChain.doFilter(request, response);
                } catch (Exception exception) {
                    if (DartsApiTrait.isInactiveUserException(exception)) {
                        writeError(response, exception);
                    } else {
                        // Log the exception and return 401 Unauthorized
                        log.error("User is invalid", exception);
                        writeError(response, exception);
                    }
                }
            }
        }

        private void writeError(HttpServletResponse response, Exception exception) {
            try {
                DartsApiTrait.writeErrorResponse(response, mapper, exception);
            } catch (IOException ex) {
                log.error("Problem parsing the problem", ex);
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        }
    }

    Jwt getJwtFromRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            String token = authHeader.replace(TOKEN_BEARER_PREFIX, "").trim();
            String issuer = JWTParser.parse(token).getJWTClaimsSet().getIssuer();

            return getNimbusJwtDecoder(issuer).decode(token);
        } catch (Exception exception) {
            log.error("Problem decoding the token", exception);
            return null;
        }
    }

    public NimbusJwtDecoder getNimbusJwtDecoder(String issuer) {
        if (decoderMap.containsKey(issuer)) {
            return decoderMap.get(issuer);
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(getJwkSetUri(issuer))
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build();
        decoderMap.put(issuer, decoder);
        return decoder;
    }

    private String getJwkSetUri(String issuer) {
        if (issuer.equals(externalAuthConfigurationProperties.getIssuerUri())) {
            return externalAuthProviderConfigurationProperties.getJwkSetUri();
        }
        if (issuer.equals(internalAuthConfigurationProperties.getIssuerUri())) {
            return internalAuthProviderConfigurationProperties.getJwkSetUri();
        }
        if (isCpMarketplaceIssuer(issuer)) {
            return cpMarketplaceAuthConfigurationProperties.getJwkSetUri();
        }
        throw new IllegalArgumentException("Issuer not found");
    }

    private boolean isCpMarketplaceIssuer(String issuer) {
        return cpMarketplaceAuthConfigurationProperties.isEnabled()
            && issuer.equals(cpMarketplaceAuthConfigurationProperties.getIssuerUri());
    }
}
