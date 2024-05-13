package uk.gov.hmcts.darts.usermanagement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.usermanagement.http.api.TranscriptionApi;
import uk.gov.hmcts.darts.usermanagement.model.TranscriptionDetails;

import java.time.OffsetDateTime;
import java.util.Optional;

import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;

@RestController
@RequiredArgsConstructor
public class TranscriptionController implements TranscriptionApi {

    private TranscriptionController transcriptionController;

    @Override
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<TranscriptionDetails> getTranscriptionsForUser(Integer userId, OffsetDateTime requestedAtFrom) {

        transcriptionController.getTranscriptionsForUser(userId, requestedAtFrom);

        return TranscriptionApi.super.getTranscriptionsForUser(userId, requestedAtFrom);
    }
}