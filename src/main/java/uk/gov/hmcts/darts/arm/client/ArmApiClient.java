package uk.gov.hmcts.darts.arm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping(value = "${darts.storage.arm-api.api-url.update-metadata-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    UpdateMetadataResponse updateMetadata(@RequestHeader(AUTHORIZATION) String bearerAuth,
                                          @RequestBody UpdateMetadataRequest updateMetadataRequest);

    @GetMapping("${darts.storage.arm-api.api-url.download-data-path}")
    @SuppressWarnings({"PMD.UseObjectForClearerAPI"})
    feign.Response downloadArmData(@RequestHeader(AUTHORIZATION) String bearerAuth,
                                   @PathVariable("cabinet_id") String cabinetId,
                                   @PathVariable("record_id") String externalRecordId,
                                   @PathVariable("file_id") String externalFileId);
}
