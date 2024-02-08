package uk.gov.hmcts.darts.common.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountServiceIntTest extends IntegrationBase {

    @Autowired
    private UserAccountStub userAccountStub;

    @Autowired
    private UserAccountService userAccountService;

    @Test
    void updateLastLoginTime() {
        // Given
        OffsetDateTime originalLastLoginTime = OffsetDateTime.parse("2023-10-27T22:00Z");
        var integrationTestUser = userAccountStub.getIntegrationTestUserAccountEntity();
        assertEquals(originalLastLoginTime, integrationTestUser.getLastLoginTime());

        // When
        userAccountService.updateLastLoginTime(integrationTestUser.getId());

        // Then
        var updatedIntegrationTestUser = userAccountStub.getIntegrationTestUserAccountEntity();
        assertNotNull(updatedIntegrationTestUser.getLastLoginTime());
        assertTrue(updatedIntegrationTestUser.getLastLoginTime().isAfter(originalLastLoginTime));
    }

}
