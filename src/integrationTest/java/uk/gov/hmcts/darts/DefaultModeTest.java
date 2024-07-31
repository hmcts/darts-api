package uk.gov.hmcts.darts;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.component.impl.UserIdentityImpl;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

@AutoConfigureMockMvc
class DefaultModeTest extends IntegrationBase {
    @Autowired
    private UserIdentity userIdentity;

    @Test
    void testUserInjection() {
        Assertions.assertTrue(userIdentity instanceof UserIdentityImpl);
    }
}