package uk.gov.hmcts.darts.arm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "arm-auth-client",
    url = "${darts.storage.arm-api.version5-2.authentication.auth-base-url}"
)
public interface ArmAuthClient {

    @PostMapping(value = "${darts.storage.arm-api.version5-2.authentication.token-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE)
    ArmTokenResponse getToken(ArmTokenRequest armTokenRequest);

}
