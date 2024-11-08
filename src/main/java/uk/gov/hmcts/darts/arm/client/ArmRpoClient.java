package uk.gov.hmcts.darts.arm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.darts.arm.client.model.rpo.ArmAsyncSearchResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedSearchesByMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProfileEntitlementResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.RecordManagementMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchResponse;
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

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-indexes-by-matter-id-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    IndexesByMatterIdResponse getIndexesByMatterId(@RequestHeader(AUTHORIZATION) String bearerToken,
                                                   @RequestBody IndexesByMatterIdRequest indexesByMatterIdRequest);

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.save-background-search-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    SaveBackgroundSearchResponse saveBackgroundSearch(@RequestHeader(AUTHORIZATION) String bearerToken,
                                                      @RequestBody SaveBackgroundSearchRequest saveBackgroundSearchRequest);

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-extended-searches-by-matter-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    ExtendedSearchesByMatterResponse getExtendedSearchesByMatter(@RequestHeader(AUTHORIZATION) String bearerToken,
                                                                 @RequestBody String body);

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-production-output-files-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    ProductionOutputFilesResponse getProductionOutputFiles(@RequestHeader(AUTHORIZATION) String bearerToken,
                                                           @RequestBody ProductionOutputFilesRequest productionOutputFilesRequest);

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.create-export-based-on-search-results-table-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    CreateExportBasedOnSearchResultsTableResponse createExportBasedOnSearchResultsTable(
        @RequestHeader(AUTHORIZATION) String bearerToken, @RequestBody CreateExportBasedOnSearchResultsTableRequest request);


}
