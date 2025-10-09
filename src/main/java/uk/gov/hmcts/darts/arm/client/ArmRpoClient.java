package uk.gov.hmcts.darts.arm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    name = "arm-rpo-client",
    url = "${darts.storage.arm-api.url}"
)
public interface ArmRpoClient {

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-record-management-matter-path}", //NOSONAR
        consumes = APPLICATION_JSON_VALUE, //NOSONAR
        produces = APPLICATION_JSON_VALUE //NOSONAR
    ) //NOSONAR
    RecordManagementMatterResponse getRecordManagementMatter(@RequestHeader(AUTHORIZATION) String bearerAuth, //NOSONAR
                                                             @RequestBody EmptyRpoRequest emptyRpoRequest); //NOSONAR


    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-storage-accounts-path}", //NOSONAR
        consumes = APPLICATION_JSON_VALUE, //NOSONAR
        produces = APPLICATION_JSON_VALUE //NOSONAR
    ) //NOSONAR
    StorageAccountResponse getStorageAccounts(@RequestHeader(AUTHORIZATION) String bearerToken, //NOSONAR
                                              @RequestBody StorageAccountRequest storageAccountRequest); //NOSONAR


    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-master-index-field-by-record-class-schema-path}", //NOSONAR
        consumes = APPLICATION_JSON_VALUE, //NOSONAR
        produces = APPLICATION_JSON_VALUE //NOSONAR
    ) //NOSONAR
    MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchema( //NOSONAR
                                                                                        @RequestHeader(AUTHORIZATION) String bearerAuth, //NOSONAR
                                                                                        @RequestBody MasterIndexFieldByRecordClassSchemaRequest  //NOSONAR
                                                                                            masterIndexFieldByRecordClassSchemaRequest); //NOSONAR

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-profile-entitlements-path}", //NOSONAR
        consumes = APPLICATION_JSON_VALUE, //NOSONAR
        produces = APPLICATION_JSON_VALUE //NOSONAR
    ) //NOSONAR
    ProfileEntitlementResponse getProfileEntitlementResponse(@RequestHeader(AUTHORIZATION) String bearerAuth, //NOSONAR
                                                             @RequestBody EmptyRpoRequest emptyRpoRequest); //NOSONAR

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.add-async-search-path}", //NOSONAR
        consumes = APPLICATION_JSON_VALUE, //NOSONAR
        produces = APPLICATION_JSON_VALUE //NOSONAR
    ) //NOSONAR
    ArmAsyncSearchResponse addAsyncSearch(@RequestHeader(AUTHORIZATION) String bearerAuth, @RequestBody String body); //NOSONAR

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-indexes-by-matter-id-path}", //NOSONAR
        consumes = APPLICATION_JSON_VALUE, //NOSONAR
        produces = APPLICATION_JSON_VALUE //NOSONAR
    ) //NOSONAR
    IndexesByMatterIdResponse getIndexesByMatterId(@RequestHeader(AUTHORIZATION) String bearerToken, //NOSONAR
                                                   @RequestBody IndexesByMatterIdRequest indexesByMatterIdRequest); //NOSONAR

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.save-background-search-path}", //NOSONAR
        consumes = APPLICATION_JSON_VALUE, //NOSONAR
        produces = APPLICATION_JSON_VALUE //NOSONAR
    ) //NOSONAR
    SaveBackgroundSearchResponse saveBackgroundSearch(@RequestHeader(AUTHORIZATION) String bearerToken, //NOSONAR
                                                      @RequestBody SaveBackgroundSearchRequest saveBackgroundSearchRequest); //NOSONAR

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-extended-searches-by-matter-path}", //NOSONAR
        consumes = APPLICATION_JSON_VALUE, //NOSONAR
        produces = APPLICATION_JSON_VALUE //NOSONAR
    ) //NOSONAR
    ExtendedSearchesByMatterResponse getExtendedSearchesByMatter(@RequestHeader(AUTHORIZATION) String bearerToken, //NOSONAR
                                                                 @RequestBody String body); //NOSONAR

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-production-output-files-path}", //NOSONAR
        consumes = APPLICATION_JSON_VALUE, //NOSONAR
        produces = APPLICATION_JSON_VALUE //NOSONAR
    ) //NOSONAR
    ProductionOutputFilesResponse getProductionOutputFiles(@RequestHeader(AUTHORIZATION) String bearerToken, //NOSONAR
                                                           @RequestBody ProductionOutputFilesRequest productionOutputFilesRequest); //NOSONAR

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.create-export-based-on-search-results-table-path}", //NOSONAR
        consumes = APPLICATION_JSON_VALUE, //NOSONAR
        produces = APPLICATION_JSON_VALUE //NOSONAR
    ) //NOSONAR
    CreateExportBasedOnSearchResultsTableResponse createExportBasedOnSearchResultsTable( //NOSONAR
                                                                                         @RequestHeader(AUTHORIZATION) String bearerToken,
                                                                                         @RequestBody CreateExportBasedOnSearchResultsTableRequest request); //NOSONAR

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.remove-production-path}", //NOSONAR
        consumes = APPLICATION_JSON_VALUE, //NOSONAR
        produces = APPLICATION_JSON_VALUE //NOSONAR
    ) //NOSONAR
    RemoveProductionResponse removeProduction(@RequestHeader(AUTHORIZATION) String bearerToken, //NOSONAR
                                              @RequestBody RemoveProductionRequest removeProductionRequest); //NOSONAR

    @PostMapping(value = "${darts.storage.arm-api.rpo-url.get-extended-productions-by-matter}", //NOSONAR
        consumes = APPLICATION_JSON_VALUE, //NOSONAR
        produces = APPLICATION_JSON_VALUE //NOSONAR
    ) //NOSONAR
    ExtendedProductionsByMatterResponse getExtendedProductionsByMatter(@RequestHeader(AUTHORIZATION) String bearerToken, //NOSONAR
                                                                       @RequestBody String body); //NOSONAR

    @GetMapping(value = "${darts.storage.arm-api.rpo-url.download-production-path}", //NOSONAR
        produces = APPLICATION_OCTET_STREAM_VALUE) //NOSONAR
    feign.Response downloadProduction(@RequestHeader(AUTHORIZATION) String bearerAuth, //NOSONAR
                                      @PathVariable("productionExportFileID") String productionExportFileId); //NOSONAR

    /**
     * Download production that should only be used in lower environments for testing purposes.
     */
    @GetMapping(value = "${darts.storage.arm-api.rpo-url.download-production-path}", //NOSONAR
        produces = APPLICATION_OCTET_STREAM_VALUE) //NOSONAR
    feign.Response downloadProduction(@RequestHeader(AUTHORIZATION) String bearerAuth, //NOSONAR
                                      @RequestHeader("EOD_IDS") String eodIds, //NOSONAR
                                      @PathVariable("productionExportFileID") String productionExportFileId); //NOSONAR

}
