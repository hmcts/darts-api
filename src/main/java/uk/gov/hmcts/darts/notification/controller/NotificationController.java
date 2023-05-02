package uk.gov.hmcts.darts.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.notification.service.NotificationService;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@RequiredArgsConstructor
@Controller
public class NotificationController implements NotificationApi {

    private final NotificationService notificationService;

    @Operation(
        operationId = "notificationCreatePost",
        summary = "Creates a notification entry in the notification table.",
        description = "description",
        tags = { "Notification" },
        responses = {
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/notification/create"
    )
    public ResponseEntity<Void> notificationCreatePost(
        @NotNull @Parameter(name = "eventId", description = "Unique ID that represents the Template to be used.", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "eventId", required = true) String eventId,
        @NotNull @Parameter(name = "caseId", description = "The Case Number", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "caseId", required = true) String caseId,
        @NotNull @Parameter(name = "emailAddresses", description = "Comma separated list of email addresses to send the notification to.", required = true, in = ParameterIn.QUERY) @Valid @RequestParam(value = "emailAddresses", required = true) String emailAddresses,
        @Parameter(name = "templateValues", description = "A Json string representing any extra template parameters that might be needed.", in = ParameterIn.QUERY) @Valid @RequestParam(value = "templateValues", required = false) String templateValues
    ) {
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId(eventId)
            .caseId(caseId)
            .emailAddresses(emailAddresses)
            .templateValues(templateValues)
            .build();

        notificationService.scheduleNotification(request);
        return new ResponseEntity<>(HttpStatus.CREATED);

    }
}
