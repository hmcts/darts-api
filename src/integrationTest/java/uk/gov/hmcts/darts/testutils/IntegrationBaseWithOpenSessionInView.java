package uk.gov.hmcts.darts.testutils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for integration tests running with H2 in Postgres compatibility mode.
 * This class also starts a containerised Redis instance
 */

public class IntegrationBaseWithOpenSessionInView extends IntegrationBase {

    @Autowired
    protected OpenInViewUtil openInViewUtil;

    @BeforeEach
    void setupSession() {
        openInViewUtil.openEntityManager();
    }

    @AfterEach
    void closeSession() {
        openInViewUtil.closeEntityManager();
    }

}