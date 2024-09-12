package uk.gov.hmcts.darts.task.runner;

import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;

public interface AutoloadingAutomatedTask {
    AbstractLockableAutomatedTask getAbstractLockableAutomatedTask();

    AutomatedTaskName getAutomatedTaskName();

    default String getTaskName() {
        return getAutomatedTaskName().getTaskName();
    }
}
