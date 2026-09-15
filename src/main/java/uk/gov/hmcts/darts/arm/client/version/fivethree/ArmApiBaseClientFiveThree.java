package uk.gov.hmcts.darts.arm.client.version.fivethree;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionResponse;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "arm-api-base-client-version5-3",
    url = "${darts.storage.arm-api.version5-3.api.api-base-url}"
)
public interface ArmApiBaseClientFiveThree {

    @PostMapping(value = "${darts.storage.arm-api.version5-3.api.remove-production-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    RemoveProductionResponse removeProduction(@RequestHeader(AUTHORIZATION) String bearerToken,
                                              @RequestBody RemoveProductionRequest removeProductionRequest);
}
