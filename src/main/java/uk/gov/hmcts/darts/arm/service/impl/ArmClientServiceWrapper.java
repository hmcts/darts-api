package uk.gov.hmcts.darts.arm.service.impl;

import feign.Response;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
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
import uk.gov.hmcts.darts.arm.client.version.fivetwo.ArmApiBaseClient;
import uk.gov.hmcts.darts.arm.client.version.fivetwo.ArmAuthClient;
import uk.gov.hmcts.darts.arm.service.ArmClientService;

/**
 * Post ARM version 5.2 implementation of the ArmClientService.
 * This implementation is activated when the property 'darts.storage.arm.arm-api.enable-arm-v5' is set to true.
 */
@Component
@ConditionalOnProperty(prefix = "darts.storage.arm-api", name = "enable-arm-v5-2-upgrade", havingValue = "true")
@AllArgsConstructor
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.UseObjectForClearerAPI"})
public class ArmClientServiceWrapper implements ArmClientService {

    private final ArmAuthClient armAuthClient;
    private final ArmApiBaseClient armApiBaseClient;

    @Override
    public ArmTokenResponse getToken(ArmTokenRequest armTokenRequest) {
        return armAuthClient.getToken(armTokenRequest);
    }

    @Override
    public UpdateMetadataResponse updateMetadata(String bearerAuth, UpdateMetadataRequest updateMetadataRequest) {
        return armApiBaseClient.updateMetadata(bearerAuth, updateMetadataRequest);
    }

    @Override
    public Response downloadArmData(String bearerAuth, String cabinetId, String externalRecordId, String externalFileId) {
        return armApiBaseClient.downloadArmData(bearerAuth, cabinetId, externalRecordId, externalFileId);
    }

    @Override
    public RecordManagementMatterResponse getRecordManagementMatter(String bearerAuth, EmptyRpoRequest emptyRpoRequest) {
        return armApiBaseClient.getRecordManagementMatter(bearerAuth, emptyRpoRequest);
    }

    @Override
    public StorageAccountResponse getStorageAccounts(String bearerToken, StorageAccountRequest storageAccountRequest) {
        return armApiBaseClient.getStorageAccounts(bearerToken, storageAccountRequest);
    }

    @Override
    public MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchema(
        String bearerAuth, MasterIndexFieldByRecordClassSchemaRequest masterIndexFieldByRecordClassSchemaRequest) {

        return armApiBaseClient.getMasterIndexFieldByRecordClassSchema(bearerAuth, masterIndexFieldByRecordClassSchemaRequest);
    }

    @Override
    public ProfileEntitlementResponse getProfileEntitlementResponse(String bearerAuth, EmptyRpoRequest emptyRpoRequest) {
        return armApiBaseClient.getProfileEntitlementResponse(bearerAuth, emptyRpoRequest);
    }

    @Override
    public ArmAsyncSearchResponse addAsyncSearch(String bearerAuth, String body) {
        return armApiBaseClient.addAsyncSearch(bearerAuth, body);
    }

    @Override
    public IndexesByMatterIdResponse getIndexesByMatterId(String bearerToken, IndexesByMatterIdRequest indexesByMatterIdRequest) {
        return armApiBaseClient.getIndexesByMatterId(bearerToken, indexesByMatterIdRequest);
    }

    @Override
    public SaveBackgroundSearchResponse saveBackgroundSearch(String bearerToken, SaveBackgroundSearchRequest saveBackgroundSearchRequest) {
        return armApiBaseClient.saveBackgroundSearch(bearerToken, saveBackgroundSearchRequest);
    }

    @Override
    public ExtendedSearchesByMatterResponse getExtendedSearchesByMatter(String bearerToken, String body) {
        return armApiBaseClient.getExtendedSearchesByMatter(bearerToken, body);
    }

    @Override
    public ProductionOutputFilesResponse getProductionOutputFiles(String bearerToken, ProductionOutputFilesRequest productionOutputFilesRequest) {
        return armApiBaseClient.getProductionOutputFiles(bearerToken, productionOutputFilesRequest);
    }

    @Override
    public CreateExportBasedOnSearchResultsTableResponse createExportBasedOnSearchResultsTable(String bearerToken,
                                                                                               CreateExportBasedOnSearchResultsTableRequest request) {
        return armApiBaseClient.createExportBasedOnSearchResultsTable(bearerToken, request);
    }

    @Override
    public RemoveProductionResponse removeProduction(String bearerToken, RemoveProductionRequest removeProductionRequest) {
        return armApiBaseClient.removeProduction(bearerToken, removeProductionRequest);
    }

    @Override
    public ExtendedProductionsByMatterResponse getExtendedProductionsByMatter(String bearerToken, String body) {
        return armApiBaseClient.getExtendedProductionsByMatter(bearerToken, body);
    }

    @Override
    public Response downloadProduction(String bearerAuth, String productionExportFileId) {
        return armApiBaseClient.downloadProduction(bearerAuth, productionExportFileId);
    }

    @Override
    public Response downloadProduction(String bearerAuth, String eodIds, String productionExportFileId) {
        return armApiBaseClient.downloadProduction(bearerAuth, eodIds, productionExportFileId);
    }

    @Override
    public AvailableEntitlementProfile availableEntitlementProfiles(String bearerAuth, EmptyRpoRequest emptyRpoRequest) {
        return armApiBaseClient.availableEntitlementProfiles(bearerAuth, emptyRpoRequest);
    }

    @Override
    public ArmTokenResponse selectEntitlementProfile(String bearerAuth, String profileId, EmptyRpoRequest emptyRpoRequest) {
        return armApiBaseClient.selectEntitlementProfile(bearerAuth, profileId, emptyRpoRequest);
    }
}
