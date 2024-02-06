package uk.gov.hmcts.darts.arm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import uk.gov.hmcts.darts.arm.client.config.ArmClientConfig;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@FeignClient(
      name = "arm-token-client",
      url = "${darts.storage.arm-api.url}",
      configuration = ArmClientConfig.class
)
public interface ArmTokenClient {

    @GetMapping(value = "${darts.storage.arm-api.token-path}",
          consumes = TEXT_PLAIN_VALUE)
    ArmTokenResponse getToken(ArmTokenRequest armTokenRequest);

}
