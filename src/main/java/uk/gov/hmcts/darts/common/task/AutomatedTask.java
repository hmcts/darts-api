package uk.gov.hmcts.darts.common.task;

public interface AutomatedTask extends Runnable {

    String getTaskName();

    String getDefaultCronExpression();
}
