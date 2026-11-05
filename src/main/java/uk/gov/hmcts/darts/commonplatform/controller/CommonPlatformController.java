package uk.gov.hmcts.darts.commonplatform.controller;


import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.commonplatform.http.api.CommonPlatformApi;
import uk.gov.hmcts.darts.commonplatform.model.CommonPlatformEventRequest;
import uk.gov.hmcts.darts.commonplatform.model.CommonPlatformSubmissionResponse;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.CPP;

@RestController
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
public class CommonPlatformController implements CommonPlatformApi {

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {CPP})
    public ResponseEntity<CommonPlatformSubmissionResponse> submitCommonPlatformDailyList(
        @RequestHeader(value = "X-Message-Id", required = true) String messageId,
        @RequestBody String body,
        @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {CPP})
    public ResponseEntity<CommonPlatformSubmissionResponse> submitCommonPlatformEvent(
        @RequestHeader(value = "X-Message-Id", required = true) String messageId,
        @RequestBody CommonPlatformEventRequest commonPlatformEventRequest,
        @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

}
