package uk.gov.hmcts.darts.arm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.darts.arm.client.model.rpo.ArmAsyncSearchResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProfileEntitlementResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.RecordManagementMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountResponse;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "arm-rpo-client",
    url = "${darts.storage.arm-api.url}"
)
public interface ArmRpoClient {

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-record-management-matter-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    RecordManagementMatterResponse getRecordManagementMatter(@RequestHeader(AUTHORIZATION) String bearerAuth);


    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-storage-accounts-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    StorageAccountResponse getStorageAccounts(@RequestHeader(AUTHORIZATION) String bearerToken,
                                              @RequestBody StorageAccountRequest storageAccountRequest);


    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-master-index-field-by-record-class-schema-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchema(
        @RequestHeader(AUTHORIZATION) String bearerAuth, @RequestBody MasterIndexFieldByRecordClassSchemaRequest masterIndexFieldByRecordClassSchemaRequest);

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-profile-entitlements-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    ProfileEntitlementResponse getProfileEntitlementResponse(@RequestHeader(AUTHORIZATION) String bearerAuth);

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.add-async-search-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    ArmAsyncSearchResponse addAsyncSearch(@RequestHeader(AUTHORIZATION) String bearerAuth, @RequestBody String body);

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.save-background-search-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    SaveBackgroundSearchResponse saveBackgroundSearch(@RequestHeader(AUTHORIZATION) String bearerToken,
                                                      @RequestBody SaveBackgroundSearchRequest saveBackgroundSearchRequest);
}
