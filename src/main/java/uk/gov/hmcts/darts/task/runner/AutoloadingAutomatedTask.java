package uk.gov.hmcts.darts.task.runner;

import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;

/**
 * When a class is annotation with this interface and is loaded into spring (is a spring component)
 * It will automatically get loaded into the AutomatedTaskService which will use the getAutomatedTaskName() method to get the database entry for this task.
 * This database entry will then be used to determine if the task should be scheduled or not.
 *
 * @see uk.gov.hmcts.darts.task.service.impl.AutomatedTaskServiceImpl
 */
public interface AutoloadingAutomatedTask {
    AbstractLockableAutomatedTask getAbstractLockableAutomatedTask();

    AutomatedTaskName getAutomatedTaskName();

    default String getTaskName() {
        return getAutomatedTaskName().getTaskName();
    }
}
