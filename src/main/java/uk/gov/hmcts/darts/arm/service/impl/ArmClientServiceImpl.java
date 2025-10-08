package uk.gov.hmcts.darts.arm.service.impl;

import feign.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmApiClient;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.ArmTokenClient;
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
import uk.gov.hmcts.darts.arm.service.ArmClientService;

/**
 * Pre ARM version 5.2 implementation of the ArmClientService.
 * This implementation is activated when the property 'darts.storage.arm.arm-api.enable-arm-v5' is set to false.
 */
@Service
@ConditionalOnProperty(prefix = "darts.storage.arm.arm-api", name = "enable-arm-v5-2-upgrade", havingValue = "false", matchIfMissing = false)
@RequiredArgsConstructor
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.UseObjectForClearerAPI"})
public class ArmClientServiceImpl implements ArmClientService {

    private final ArmTokenClient armTokenClient;
    private final ArmApiClient armApiClient;
    private final ArmRpoClient armRpoClient;

    @Override
    public ArmTokenResponse getToken(ArmTokenRequest armTokenRequest) {
        return armTokenClient.getToken(armTokenRequest);
    }

    @Override
    public UpdateMetadataResponse updateMetadata(String bearerAuth, UpdateMetadataRequest updateMetadataRequest) {
        return armApiClient.updateMetadata(bearerAuth, updateMetadataRequest);
    }

    @Override
    public Response downloadArmData(String bearerAuth, String cabinetId, String externalRecordId, String externalFileId) {
        return armApiClient.downloadArmData(bearerAuth, cabinetId, externalRecordId, externalFileId);
    }

    @Override
    public RecordManagementMatterResponse getRecordManagementMatter(String bearerAuth, EmptyRpoRequest emptyRpoRequest) {
        return armRpoClient.getRecordManagementMatter(bearerAuth, emptyRpoRequest);
    }

    @Override
    public StorageAccountResponse getStorageAccounts(String bearerToken, StorageAccountRequest storageAccountRequest) {
        return armRpoClient.getStorageAccounts(bearerToken, storageAccountRequest);
    }

    @Override
    public MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchema(
        String bearerAuth, MasterIndexFieldByRecordClassSchemaRequest masterIndexFieldByRecordClassSchemaRequest) {
        return armRpoClient.getMasterIndexFieldByRecordClassSchema(bearerAuth, masterIndexFieldByRecordClassSchemaRequest);
    }

    @Override
    public ProfileEntitlementResponse getProfileEntitlementResponse(String bearerAuth, EmptyRpoRequest emptyRpoRequest) {
        return armRpoClient.getProfileEntitlementResponse(bearerAuth, emptyRpoRequest);
    }

    @Override
    public ArmAsyncSearchResponse addAsyncSearch(String bearerAuth, String body) {
        return armRpoClient.addAsyncSearch(bearerAuth, body);
    }

    @Override
    public IndexesByMatterIdResponse getIndexesByMatterId(String bearerToken, IndexesByMatterIdRequest indexesByMatterIdRequest) {
        return armRpoClient.getIndexesByMatterId(bearerToken, indexesByMatterIdRequest);
    }

    @Override
    public SaveBackgroundSearchResponse saveBackgroundSearch(String bearerToken, SaveBackgroundSearchRequest saveBackgroundSearchRequest) {
        return armRpoClient.saveBackgroundSearch(bearerToken, saveBackgroundSearchRequest);
    }

    @Override
    public ExtendedSearchesByMatterResponse getExtendedSearchesByMatter(String bearerToken, String body) {
        return armRpoClient.getExtendedSearchesByMatter(bearerToken, body);
    }

    @Override
    public ProductionOutputFilesResponse getProductionOutputFiles(String bearerToken, ProductionOutputFilesRequest productionOutputFilesRequest) {
        return armRpoClient.getProductionOutputFiles(bearerToken, productionOutputFilesRequest);
    }

    @Override
    public CreateExportBasedOnSearchResultsTableResponse createExportBasedOnSearchResultsTable(String bearerToken,
                                                                                               CreateExportBasedOnSearchResultsTableRequest request) {
        return armRpoClient.createExportBasedOnSearchResultsTable(bearerToken, request);
    }

    @Override
    public RemoveProductionResponse removeProduction(String bearerToken, RemoveProductionRequest removeProductionRequest) {
        return armRpoClient.removeProduction(bearerToken, removeProductionRequest);
    }

    @Override
    public ExtendedProductionsByMatterResponse getExtendedProductionsByMatter(String bearerToken, String body) {
        return armRpoClient.getExtendedProductionsByMatter(bearerToken, body);
    }

    @Override
    public Response downloadProduction(String bearerAuth, String productionExportFileId) {
        return armRpoClient.downloadProduction(bearerAuth, productionExportFileId);
    }

    @Override
    public Response downloadProduction(String bearerAuth, String eodIds, String productionExportFileId) {
        return armRpoClient.downloadProduction(bearerAuth, eodIds, productionExportFileId);
    }

    @Override
    public AvailableEntitlementProfile availableEntitlementProfiles(String bearerAuth, EmptyRpoRequest emptyRpoRequest) {
        return armTokenClient.availableEntitlementProfiles(bearerAuth, emptyRpoRequest);
    }

    @Override
    public ArmTokenResponse selectEntitlementProfile(String bearerAuth, String profileId, EmptyRpoRequest emptyRpoRequest) {
        return armTokenClient.selectEntitlementProfile(bearerAuth, profileId, emptyRpoRequest);
    }
}
