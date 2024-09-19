package uk.gov.hmcts.darts.task.runner;

import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;

public interface AutoloadingManualTask {
    AbstractLockableAutomatedTask getAbstractLockableAutomatedTask();
}
