package uk.gov.hmcts.darts.retention.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

public class ApplyRetentionCaseAssociatedObjectsProcessorIntTest extends IntegrationBase {

    @Autowired
    ApplyRetentionCaseAssociatedObjectsProcessor processor;

    @Test
    void testSuccessfullyLoads() {
    }
}
