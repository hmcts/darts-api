package uk.gov.hmcts.darts.authorisation.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailAddressFromTokenUtilTest {

    @Test
    void getFromEmailClaim() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("email", "integrationtest.user@example.com")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertEquals("integrationtest.user@example.com", EmailAddressFromTokenUtil.getEmailAddressFromToken(jwt));

    }

    @Test
    void getFromEmailsClaim() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of("integrationtest.user@example.com"))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertEquals("integrationtest.user@example.com", EmailAddressFromTokenUtil.getEmailAddressFromToken(jwt));

    }

    @Test
    void getFromEmailsClaimWhenMultiple() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("email", "email1@test.com")
            .claim("preferred_username", "email2@test.com")
            .claim("emails", List.of("email3@test.com"))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertEquals("email3@test.com", EmailAddressFromTokenUtil.getEmailAddressFromToken(jwt));

    }

    @Test
    void getFromEmailClaimWhenMultiple() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("email", "email1@test.com")
            .claim("preferred_username", "email2@test.com")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertEquals("email1@test.com", EmailAddressFromTokenUtil.getEmailAddressFromToken(jwt));

    }

    @Test
    void getUserAccountShouldThrowExceptionWhenUnexpectedNumberOfEmails() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of("test.user@example.com", "test.user2@example.com"))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        var exception = assertThrows(IllegalStateException.class, () -> EmailAddressFromTokenUtil.getEmailAddressFromToken(jwt));
        assertEquals("Unexpected number of email addresses: 2", exception.getMessage());
    }

    @Test
    void getUserAccountShouldThrowExceptionWhenMissingEmailsClaim() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        var exception = assertThrows(IllegalStateException.class, () -> EmailAddressFromTokenUtil.getEmailAddressFromToken(jwt));
        assertEquals("Could not obtain email address from principal", exception.getMessage());
    }

    @Test
    void getUserAccountGetEmailAddressForInternalUser() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("preferred_username", "integrationtest.user@example.com")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertEquals("integrationtest.user@example.com", EmailAddressFromTokenUtil.getEmailAddressFromToken(jwt));
    }

    @Test
    void getUserAccountShouldThrowExceptionWithEmptyClaims() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of(1))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        var exception = assertThrows(IllegalStateException.class, () -> EmailAddressFromTokenUtil.getEmailAddressFromToken(jwt));
        assertEquals("Could not obtain email address from principal", exception.getMessage());
    }
}