package uk.gov.hmcts.darts.authorisation.component.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest()
class UserIdentityImplTest extends IntegrationBase {

    @Autowired
    private UserIdentity userIdentity;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private DartsDatabaseStub dartsDatabaseStub;

    @Test
    void getEmailAddress() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of("test.user@example.com"))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertEquals("test.user@example.com", userIdentity.getEmailAddress());
    }

    @Test
    void getEmailAddressShouldThrowExceptionWhenUnexpectedNumberOfEmails() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of("test.user@example.com", "test.user2@example.com"))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        var exception = assertThrows(IllegalStateException.class, () -> userIdentity.getEmailAddress());
        assertEquals("Unexpected number of email addresses: 2", exception.getMessage());
    }

    @Test
    void getEmailAddressShouldThrowExceptionWhenMissingEmailsClaim() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        var exception = assertThrows(IllegalStateException.class, () -> userIdentity.getEmailAddress());
        assertEquals("Could not obtain email address from principal", exception.getMessage());
    }

    @Test
    void getEmailAddressForInternalUser() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("preferred_username", "test.user@example.com")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertEquals("test.user@example.com", userIdentity.getEmailAddress());
    }

    @Test
    void getEmailAddressEmptyClaims() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of(1))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        var exception = assertThrows(IllegalStateException.class, () -> userIdentity.getEmailAddress());
        assertEquals("Could not obtain email address from principal", exception.getMessage());
    }

    @Test
    void getUserAccount() {
        String email = "integrationtest.user@example.com";
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of(email))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertEquals(email, userIdentity.getEmailAddress());

        UserAccountEntity testUser = dartsDatabaseStub.getUserAccountStub().getIntegrationTestUserAccountEntity();

        UserAccountEntity currentUser = userIdentity.getUserAccount();
        assertEquals(testUser.getId(), currentUser.getId());
    }

    @Test
    void getUserAccountForNonExistingEmailAddressThrowsIllegalStateException() {
        String email = "non-existing-user@example.com";
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of(email))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertEquals(email, userIdentity.getEmailAddress());

        dartsDatabaseStub.getUserAccountStub().getIntegrationTestUserAccountEntity();

        assertThrows(
            IllegalStateException.class,
            () -> userIdentity.getUserAccount()
        );

    }
}
