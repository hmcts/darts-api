package uk.gov.hmcts.darts.task.runner;

import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;

public interface AutomatedTask extends Runnable {

    String getTaskName();

    void run(boolean isManualRun);

    default void run() {
        run(false);
    }

    AutomatedTaskStatus getAutomatedTaskStatus();

    String getLastCronExpression();

    void setLastCronExpression(String cronExpression);

}
