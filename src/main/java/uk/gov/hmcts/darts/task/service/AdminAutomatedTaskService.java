package uk.gov.hmcts.darts.task.service;

import uk.gov.hmcts.darts.tasks.model.AutomatedTaskCronExpressionPatchRequest;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskCronExpressionPostRequest;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskCronExpressionScheduleResponse;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskPatch;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskSummary;
import uk.gov.hmcts.darts.tasks.model.DetailedAutomatedTask;

import java.util.List;

public interface AdminAutomatedTaskService {

    List<AutomatedTaskSummary> getAllAutomatedTasksSummaries();

    DetailedAutomatedTask getAutomatedTaskById(Integer taskId);

    void runAutomatedTask(Integer taskId);

    DetailedAutomatedTask updateAutomatedTask(Integer taskId, AutomatedTaskPatch automatedTaskPatch);

    List<AutomatedTaskCronExpressionScheduleResponse> getAutomatedTaskCronExpressionSchedule(
        Integer taskId, AutomatedTaskCronExpressionPostRequest automatedTaskCronExpressionPostRequest);

    void updateAutomatedTaskCronExpressionSchedule(
        Integer taskId, AutomatedTaskCronExpressionPatchRequest automatedTaskCronExpressionPatchRequest);
}
