package uk.gov.hmcts.darts.arm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.darts.arm.model.rpo.RecordManagementMatterResponse;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "arm-rpo-client",
    url = "${darts.storage.arm-api.url}"
)
public interface ArmRpoClient {

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-record-management-matter}",
        produces = APPLICATION_JSON_VALUE
    )
    RecordManagementMatterResponse getRecordManagementMatter(@RequestHeader(AUTHORIZATION) String bearerAuth);
}
