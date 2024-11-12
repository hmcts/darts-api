package uk.gov.hmcts.darts.task.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.task.service.AdminAutomatedTaskService;
import uk.gov.hmcts.darts.tasks.http.api.TasksApi;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskPatch;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskSummary;
import uk.gov.hmcts.darts.tasks.model.DetailedAutomatedTask;

import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
public class AutomatedTasksController implements TasksApi {

    private final AdminAutomatedTaskService adminAutomatedTaskService;

    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN})
    @Override
    public ResponseEntity<List<AutomatedTaskSummary>> getAutomatedTasks() {
        return new ResponseEntity<>(adminAutomatedTaskService.getAllAutomatedTasksSummaries(), HttpStatus.OK);
    }

    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN})
    @Override
    public ResponseEntity<DetailedAutomatedTask> getAutomatedTaskById(Integer taskId) {
        return new ResponseEntity<>(adminAutomatedTaskService.getAutomatedTaskById(taskId), HttpStatus.OK);
    }

    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, securityRoles = {SUPER_ADMIN}, globalAccessSecurityRoles = {SUPER_ADMIN})
    @Override
    public ResponseEntity<Void> runAutomatedTask(Integer taskId) {
        adminAutomatedTaskService.runAutomatedTask(taskId);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, securityRoles = {SUPER_ADMIN}, globalAccessSecurityRoles = {SUPER_ADMIN})
    @Override
    public ResponseEntity<DetailedAutomatedTask> patchAutomatedTask(Integer taskId, AutomatedTaskPatch automatedTaskPatch) {
        var automatedTask = adminAutomatedTaskService.updateAutomatedTask(taskId, automatedTaskPatch);
        return new ResponseEntity<>(automatedTask, HttpStatus.OK);
    }
}

