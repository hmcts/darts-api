package uk.gov.hmcts.darts.arm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "arm-api-client",
    url = "${darts.storage.arm-api.url}"
)
public interface ArmApiClient {

    @PostMapping(value = "${darts.storage.arm-api.update-metadata-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    ResponseEntity<UpdateMetadataResponse> updateMetadata(@RequestHeader(AUTHORIZATION) String bearerAuth,
                                                          @RequestBody UpdateMetadataRequest updateMetadataRequest);

}
