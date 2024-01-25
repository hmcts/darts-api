package uk.gov.hmcts.darts.arm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;

@FeignClient(
    name = "arm-token-client",
    url = "${darts.storage.arm.token-base-url}",
    configuration = ArmClientConfig.class
)
public interface ArmTokenClient {

    @GetMapping(path = "/api/v1/token", consumes = MediaType.TEXT_PLAIN_VALUE)
    ArmTokenResponse getToken(ArmTokenRequest armTokenRequest);

}
