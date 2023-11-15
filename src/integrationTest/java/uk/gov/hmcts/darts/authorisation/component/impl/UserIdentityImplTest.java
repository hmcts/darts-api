package uk.gov.hmcts.darts.authorisation.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.USER_DETAILS_INVALID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.CPP;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.XHIBIT;

@SpringBootTest()
class UserIdentityImplTest extends IntegrationBase {

    @Autowired
    private UserIdentity userIdentity;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private DartsDatabaseStub dartsDatabaseStub;

    @Autowired
    private AuthorisationStub authorisationStub;


    @BeforeEach
    void beforeEach() {
        authorisationStub.givenTestSchema();
    }


    @Test
    void getUserAccountGetEmailAddress() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of("integrationtest.user@example.com"))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertEquals("integrationtest.user@example.com", userIdentity.getUserAccount().getEmailAddress());
    }

    @Test
    void getUserAccountShouldThrowExceptionWhenUnexpectedNumberOfEmails() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of("test.user@example.com", "test.user2@example.com"))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        var exception = assertThrows(IllegalStateException.class, () -> userIdentity.getUserAccount());
        assertEquals("Unexpected number of email addresses: 2", exception.getMessage());
    }

    @Test
    void getUserAccountShouldThrowExceptionWhenMissingEmailsClaim() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        var exception = assertThrows(IllegalStateException.class, () -> userIdentity.getUserAccount());
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

        assertEquals("integrationtest.user@example.com", userIdentity.getUserAccount().getEmailAddress());
    }

    @Test
    void getUserAccountShouldThrowExceptionWithEmptyClaims() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of(1))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        var exception = assertThrows(IllegalStateException.class, () -> userIdentity.getUserAccount());
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

        assertEquals(email, userIdentity.getUserAccount().getEmailAddress());

        UserAccountEntity testUser = dartsDatabaseStub.getUserAccountStub().getIntegrationTestUserAccountEntity();

        UserAccountEntity currentUser = userIdentity.getUserAccount();
        assertEquals(testUser.getId(), currentUser.getId());
    }

    @Test
    void getUserAccountForNonExistingEmailAddressThrowsException() {
        String email = "non-existing-user@example.com";
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of(email))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        var exception = assertThrows(
            DartsApiException.class,
            () -> userIdentity.getUserAccount()
        );

        assertEquals(USER_DETAILS_INVALID.getTitle(), exception.getMessage());
        assertEquals(USER_DETAILS_INVALID, exception.getError());

    }

    @Test
    @Transactional
    void getGuid() {
        String guid = UUID.randomUUID().toString();
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("oid", guid)
            .build();

        dartsDatabaseStub.getUserAccountStub().createXhibitExternalUser(guid, null);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertEquals(guid, userIdentity.getUserAccount().getAccountGuid());
    }

    @Test
    @Transactional
    void userHasGlobalAccess() {
        String guid = UUID.randomUUID().toString();
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("oid", guid)
            .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        dartsDatabaseStub.getUserAccountStub().createXhibitExternalUser(guid, null);

        assertTrue(userIdentity.userHasGlobalAccess(Set.of(XHIBIT, CPP)));

    }

    @Test
    @Transactional
    void userHasGlobalAccessReturnsFalseWhenUserHasNoGlobalAccess() {
        String guid = UUID.randomUUID().toString();
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("oid", guid)
            .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        dartsDatabaseStub.getUserAccountStub().createAuthorisedIntegrationTestUser(null);

        assertFalse(userIdentity.userHasGlobalAccess(Set.of(XHIBIT, CPP)));

    }
}
