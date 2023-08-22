package uk.gov.hmcts.darts.task.runner;

import net.javacrumbs.shedlock.core.LockConfiguration;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;

public interface AutomatedTask extends Runnable {

    String getTaskName();

    AutomatedTaskStatus getAutomatedTaskStatus();

    LockConfiguration getLockConfiguration();

    String getLastCronExpression();

    void setLastCronExpression(String cronExpression);

}
