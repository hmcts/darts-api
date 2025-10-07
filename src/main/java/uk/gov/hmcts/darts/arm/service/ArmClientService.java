package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
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

public interface ArmClientService {

    ArmTokenResponse getToken(ArmTokenRequest armTokenRequest);

    UpdateMetadataResponse updateMetadata(String bearerAuth, UpdateMetadataRequest updateMetadataRequest);

    @SuppressWarnings({"PMD.UseObjectForClearerAPI"})
    feign.Response downloadArmData(String bearerAuth, String cabinetId, String externalRecordId, String externalFileId);

    RecordManagementMatterResponse getRecordManagementMatter(String bearerAuth, EmptyRpoRequest emptyRpoRequest);

    StorageAccountResponse getStorageAccounts(String bearerToken, StorageAccountRequest storageAccountRequest);

    MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchema(
        String bearerAuth,
        MasterIndexFieldByRecordClassSchemaRequest masterIndexFieldByRecordClassSchemaRequest);

    ProfileEntitlementResponse getProfileEntitlementResponse(String bearerAuth, EmptyRpoRequest emptyRpoRequest);

    ArmAsyncSearchResponse addAsyncSearch(String bearerAuth, String body);

    IndexesByMatterIdResponse getIndexesByMatterId(String bearerToken, IndexesByMatterIdRequest indexesByMatterIdRequest);

    SaveBackgroundSearchResponse saveBackgroundSearch(String bearerToken,
                                                      SaveBackgroundSearchRequest saveBackgroundSearchRequest);

    ExtendedSearchesByMatterResponse getExtendedSearchesByMatter(String bearerToken,
                                                                 String body);

    ProductionOutputFilesResponse getProductionOutputFiles(String bearerToken,
                                                           ProductionOutputFilesRequest productionOutputFilesRequest);

    CreateExportBasedOnSearchResultsTableResponse createExportBasedOnSearchResultsTable(
        String bearerToken, CreateExportBasedOnSearchResultsTableRequest request);

    RemoveProductionResponse removeProduction(String bearerToken,
                                              RemoveProductionRequest removeProductionRequest);

    ExtendedProductionsByMatterResponse getExtendedProductionsByMatter(String bearerToken,
                                                                       String body);

    feign.Response downloadProduction(String bearerAuth, String productionExportFileId);

    feign.Response downloadProduction(String bearerAuth, String eodIds, String productionExportFileId);

    AvailableEntitlementProfile availableEntitlementProfiles(String bearerAuth, EmptyRpoRequest emptyRpoRequest);

    ArmTokenResponse selectEntitlementProfile(String bearerAuth, String profileId, EmptyRpoRequest emptyRpoRequest);
}
