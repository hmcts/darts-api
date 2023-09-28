package uk.gov.hmcts.darts.audio.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audio.component.AudioRequestSummaryMapper;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audiorequests.api.AudioRequestsApi;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestSummary;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;

import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.MEDIA_REQUEST_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.LANGUAGE_SHOP_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;

@RestController
@RequiredArgsConstructor
public class AudioRequestsController implements AudioRequestsApi {

    private final MediaRequestService mediaRequestService;
    private final AudioRequestSummaryMapper audioRequestSummaryMapper;

    @Override
    public ResponseEntity<List<AudioRequestSummary>> getYourAudio(Integer userId, Boolean expired) {

        return new ResponseEntity<>(audioRequestSummaryMapper.mapToAudioRequestSummary(
            mediaRequestService.viewAudioRequests(userId, expired)), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_REQUEST_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS})
    public ResponseEntity<Void> deleteAudioRequest(Integer mediaRequestId) {
        mediaRequestService.deleteAudioRequest(mediaRequestId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
