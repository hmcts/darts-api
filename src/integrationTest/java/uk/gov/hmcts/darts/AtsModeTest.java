package uk.gov.hmcts.darts;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.component.impl.AtsUserIdentityImpl;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

@AutoConfigureMockMvc
@TestPropertySource(properties = {"ATS_MODE = true"})
class AtsModeTest extends IntegrationBase {

    @Autowired
    private UserIdentity userIdentity;

    @Test
    void testAtsIjection() {
        Assertions.assertTrue(userIdentity instanceof AtsUserIdentityImpl);
    }
}