package uk.gov.hmcts.darts.task.runner;

import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;

/**
 * When a class is annotation with this interface and is loaded into spring (is a spring component)
 * It will automatically get loaded into the ManualTaskService which will use the getAbstractLockableAutomatedTask() method to set the task as a manual task.
 *
 * @see uk.gov.hmcts.darts.task.service.impl.ManualTaskService
 */
@FunctionalInterface
public interface AutoloadingManualTask {
    AbstractLockableAutomatedTask getAbstractLockableAutomatedTask();
}
