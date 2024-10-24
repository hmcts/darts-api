package uk.gov.hmcts.darts.arm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.darts.arm.client.config.ArmClientConfig;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@FeignClient(
    name = "arm-token-client",
    url = "${darts.storage.arm-api.url}",
    configuration = ArmClientConfig.class
)
public interface ArmTokenClient {

    @PostMapping(value = "${darts.storage.arm-api.authentication-url.token-path}",
        consumes = TEXT_PLAIN_VALUE,
        produces = APPLICATION_JSON_VALUE)
    ArmTokenResponse getToken(ArmTokenRequest armTokenRequest);

    @PostMapping(value = "${darts.storage.arm-api.authentication-url.available-entitlement-profiles-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE)
    @SuppressWarnings({"PMD.UseObjectForClearerAPI"})
    AvailableEntitlementProfile availableEntitlementProfiles(@RequestHeader(AUTHORIZATION) String bearerAuth);

    @PostMapping(value = "${darts.storage.arm-api.authentication-url.select-entitlement-profile-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE)
    @SuppressWarnings({"PMD.UseObjectForClearerAPI"})
    ArmTokenResponse selectEntitlementProfile(@RequestHeader(AUTHORIZATION) String bearerAuth,
                                              @PathVariable("profile_id") String profileId);

}
