package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

class DetsToArmAutomatedTaskTest extends PostgresIntegrationBase {

    @Autowired
    private DetsToArmAutomatedTask detsToArmAutomatedTask;

    @Test
    void runTask() {
        detsToArmAutomatedTask.preRunTask();
        detsToArmAutomatedTask.runTask();
    }
}