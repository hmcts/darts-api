package uk.gov.hmcts.darts.arm.client.version.fivetwo;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ArmAsyncSearchResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedProductionsByMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedSearchesByMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProductionOutputFilesResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProfileEntitlementResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.RecordManagementMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.SaveBackgroundSearchResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountResponse;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@FeignClient(
    name = "arm-api-base-client",
    url = "${darts.storage.arm-api.version5-2.api.api-base-url}"
)
public interface ArmApiBaseClient {

    @PostMapping(value = "${darts.storage.arm-api.version5-2.api.update-metadata-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    UpdateMetadataResponse updateMetadata(@RequestHeader(AUTHORIZATION) String bearerAuth,
                                          @RequestBody UpdateMetadataRequest updateMetadataRequest);

    @GetMapping("${darts.storage.arm-api.version5-2.api.download-data-path}")
    @SuppressWarnings({"PMD.UseObjectForClearerAPI"})
    feign.Response downloadArmData(@RequestHeader(AUTHORIZATION) String bearerAuth,
                                   @PathVariable("cabinet_id") String cabinetId,
                                   @PathVariable("record_id") String externalRecordId,
                                   @PathVariable("file_id") String externalFileId);

    @PostMapping(value = "${darts.storage.arm-api.version5-2.api.get-record-management-matter-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    RecordManagementMatterResponse getRecordManagementMatter(@RequestHeader(AUTHORIZATION) String bearerAuth,
                                                             @RequestBody EmptyRpoRequest emptyRpoRequest);


    @PostMapping(value = "${darts.storage.arm-api.version5-2.api.get-storage-accounts-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    StorageAccountResponse getStorageAccounts(@RequestHeader(AUTHORIZATION) String bearerToken,
                                              @RequestBody StorageAccountRequest storageAccountRequest);


    @PostMapping(value = "${darts.storage.arm-api.version5-2.api.get-master-index-field-by-record-class-schema-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchema(
        @RequestHeader(AUTHORIZATION) String bearerAuth, @RequestBody MasterIndexFieldByRecordClassSchemaRequest masterIndexFieldByRecordClassSchemaRequest);

    @PostMapping(value = "${darts.storage.arm-api.version5-2.api.get-profile-entitlements-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    ProfileEntitlementResponse getProfileEntitlementResponse(@RequestHeader(AUTHORIZATION) String bearerAuth,
                                                             @RequestBody EmptyRpoRequest emptyRpoRequest);

    @PostMapping(value = "${darts.storage.arm-api.version5-2.api.add-async-search-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    ArmAsyncSearchResponse addAsyncSearch(@RequestHeader(AUTHORIZATION) String bearerAuth, @RequestBody String body);

    @PostMapping(value = "${darts.storage.arm-api.version5-2.api.get-indexes-by-matter-id-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    IndexesByMatterIdResponse getIndexesByMatterId(@RequestHeader(AUTHORIZATION) String bearerToken,
                                                   @RequestBody IndexesByMatterIdRequest indexesByMatterIdRequest);

    @PostMapping(value = "${darts.storage.arm-api.version5-2.api.save-background-search-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    SaveBackgroundSearchResponse saveBackgroundSearch(@RequestHeader(AUTHORIZATION) String bearerToken,
                                                      @RequestBody SaveBackgroundSearchRequest saveBackgroundSearchRequest);

    @PostMapping(value = "${darts.storage.arm-api.version5-2.api.get-extended-searches-by-matter-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    ExtendedSearchesByMatterResponse getExtendedSearchesByMatter(@RequestHeader(AUTHORIZATION) String bearerToken,
                                                                 @RequestBody String body);

    @PostMapping(value = "${darts.storage.arm-api.version5-2.api.get-production-output-files-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    ProductionOutputFilesResponse getProductionOutputFiles(@RequestHeader(AUTHORIZATION) String bearerToken,
                                                           @RequestBody ProductionOutputFilesRequest productionOutputFilesRequest);

    @PostMapping(value = "${darts.storage.arm-api.version5-2.api.rpo-url.create-export-based-on-search-results-table-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    CreateExportBasedOnSearchResultsTableResponse createExportBasedOnSearchResultsTable(
        @RequestHeader(AUTHORIZATION) String bearerToken, @RequestBody CreateExportBasedOnSearchResultsTableRequest request);


    @PostMapping(value = "${darts.storage.arm-api.version5-2.api.rpo-url.remove-production-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    RemoveProductionResponse removeProduction(@RequestHeader(AUTHORIZATION) String bearerToken,
                                              @RequestBody RemoveProductionRequest removeProductionRequest);

    @PostMapping(value = "${darts.storage.arm-api.version5-2.api.get-extended-productions-by-matter}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    ExtendedProductionsByMatterResponse getExtendedProductionsByMatter(@RequestHeader(AUTHORIZATION) String bearerToken,
                                                                       @RequestBody String body);

    @GetMapping(value = "${darts.storage.arm-api.version5-2.api.download-production-path}",
        produces = APPLICATION_OCTET_STREAM_VALUE)
    feign.Response downloadProduction(@RequestHeader(AUTHORIZATION) String bearerAuth,
                                      @PathVariable("productionExportFileID") String productionExportFileId);

    /**
     * Download production that should only be used in lower environments for testing purposes.
     */
    @GetMapping(value = "${darts.storage.arm-api.version5-2.api.download-production-path}",
        produces = APPLICATION_OCTET_STREAM_VALUE)
    feign.Response downloadProduction(@RequestHeader(AUTHORIZATION) String bearerAuth,
                                      @RequestHeader("EOD_IDS") String eodIds,
                                      @PathVariable("productionExportFileID") String productionExportFileId);

    @PostMapping(value = "${darts.storage.arm-api.version5-2.api.available-entitlement-profiles-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE)
    AvailableEntitlementProfile availableEntitlementProfiles(@RequestHeader(AUTHORIZATION) String bearerAuth,
                                                             @RequestBody EmptyRpoRequest emptyRpoRequest);

    @PostMapping(value = "${darts.storage.arm-api.version5-2.api.select-entitlement-profile-path}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE)
    ArmTokenResponse selectEntitlementProfile(@RequestHeader(AUTHORIZATION) String bearerAuth,
                                              @PathVariable("profile_id") String profileId,
                                              @RequestBody EmptyRpoRequest emptyRpoRequest);

}
