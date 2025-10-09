package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ArmAsyncSearchResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.CreateExportBasedOnSearchResultsTableResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
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
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArmClientServiceWrapperTest {
    private ArmAuthClient armAuthClient;
    private ArmApiBaseClient armApiClient;
    private ArmClientServiceWrapper service;

    @BeforeEach
    void setUp() {
        armAuthClient = mock(ArmAuthClient.class);
        armApiClient = mock(ArmApiBaseClient.class);
        service = new ArmClientServiceWrapper(armAuthClient, armApiClient);
    }

    @Test
    void getToken_returnsResponse() {
        ArmTokenRequest request = ArmTokenRequest.builder().build();
        ArmTokenResponse response = ArmTokenResponse.builder().build();
        when(armAuthClient.getToken(request)).thenReturn(response);

        ArmTokenResponse result = service.getToken(request);

        assertEquals(response, result);
        verify(armAuthClient).getToken(request);
    }

    @Test
    void updateMetadata_returnsResponse() {
        UpdateMetadataRequest request = UpdateMetadataRequest.builder()
            .itemId("itemId")
            .build();
        UpdateMetadataResponse response = UpdateMetadataResponse.builder().build();
        when(armApiClient.updateMetadata("Bearer token", request)).thenReturn(response);

        UpdateMetadataResponse result = service.updateMetadata("Bearer token", request);

        assertEquals(response, result);
        verify(armApiClient).updateMetadata("Bearer token", request);
    }

    @Test
    void getRecordManagementMatter_returnsResponse() {
        EmptyRpoRequest request = EmptyRpoRequest.builder().build();
        RecordManagementMatterResponse response = new RecordManagementMatterResponse();
        when(armApiClient.getRecordManagementMatter("Bearer token", request)).thenReturn(response);

        RecordManagementMatterResponse result = service.getRecordManagementMatter("Bearer token", request);

        assertEquals(response, result);
        verify(armApiClient).getRecordManagementMatter("Bearer token", request);
    }

    @Test
    void getStorageAccounts_returnsResponse() {
        StorageAccountRequest request = StorageAccountRequest.builder().build();
        StorageAccountResponse response = new StorageAccountResponse();
        when(armApiClient.getStorageAccounts("Bearer token", request)).thenReturn(response);

        StorageAccountResponse result = service.getStorageAccounts("Bearer token", request);

        assertEquals(response, result);
        verify(armApiClient).getStorageAccounts("Bearer token", request);
    }

    @Test
    void createExportBasedOnSearchResultsTable_returnsResponse() {
        CreateExportBasedOnSearchResultsTableRequest request = createRequestForCreateExportBasedOnSearchResultsTable(
            List.of(
                MasterIndexFieldByRecordClassSchema.builder()
                    .masterIndexField("masterIndexField1")
                    .displayName("displayName1")
                    .propertyName("propertyName1")
                    .propertyType("propertyType1")
                    .isMasked(true)
                    .build(),
                MasterIndexFieldByRecordClassSchema.builder()
                    .masterIndexField("masterIndexField2")
                    .displayName("displayName2")
                    .propertyName("propertyName2")
                    .propertyType("propertyType2")
                    .isMasked(false)
                    .build()
            ),
            "searchId",
            100,
            "productionName",
            "storageAccountId"
        );
        CreateExportBasedOnSearchResultsTableResponse response = new CreateExportBasedOnSearchResultsTableResponse();
        when(armApiClient.createExportBasedOnSearchResultsTable("Bearer token", request)).thenReturn(response);

        CreateExportBasedOnSearchResultsTableResponse result = service.createExportBasedOnSearchResultsTable("Bearer token", request);

        assertEquals(response, result);
        verify(armApiClient).createExportBasedOnSearchResultsTable("Bearer token", request);
    }

    @Test
    void removeProduction_returnsResponse() {
        RemoveProductionRequest request = RemoveProductionRequest.builder().build();
        RemoveProductionResponse response = new RemoveProductionResponse();
        when(armApiClient.removeProduction("Bearer token", request)).thenReturn(response);

        RemoveProductionResponse result = service.removeProduction("Bearer token", request);

        assertEquals(response, result);
        verify(armApiClient).removeProduction("Bearer token", request);
    }

    @Test
    void downloadArmData_returnsResponse() {
        service.downloadArmData("Bearer token", "cabinetId", "externalRecordId", "externalFileId");

        verify(armApiClient).downloadArmData("Bearer token", "cabinetId", "externalRecordId", "externalFileId");
    }

    @Test
    void getMasterIndexFieldByRecordClassSchema_returnsResponse() {
        MasterIndexFieldByRecordClassSchemaRequest request = MasterIndexFieldByRecordClassSchemaRequest.builder().build();
        MasterIndexFieldByRecordClassSchemaResponse response = getMasterIndexFieldByRecordClassSchemaResponse("propertyName1", "propertyName2");
        when(armApiClient.getMasterIndexFieldByRecordClassSchema("Bearer token", request)).thenReturn(response);

        service.getMasterIndexFieldByRecordClassSchema("Bearer token", request);

        verify(armApiClient).getMasterIndexFieldByRecordClassSchema("Bearer token", request);
    }

    @Test
    void getProfileEntitlementResponse_returnsResponse() {
        EmptyRpoRequest request = EmptyRpoRequest.builder().build();
        ProfileEntitlementResponse response = new ProfileEntitlementResponse();
        when(armApiClient.getProfileEntitlementResponse("Bearer token", request)).thenReturn(response);

        service.getProfileEntitlementResponse("Bearer token", request);

        verify(armApiClient).getProfileEntitlementResponse("Bearer token", request);
    }

    @Test
    void addAsyncSearch_returnsResponse() {
        ArmAsyncSearchResponse armAsyncSearchResponse = new ArmAsyncSearchResponse();
        when(armApiClient.addAsyncSearch("Bearer token", "body")).thenReturn(armAsyncSearchResponse);

        service.addAsyncSearch("Bearer token", "body");

        verify(armApiClient).addAsyncSearch("Bearer token", "body");
    }

    @Test
    void getIndexesByMatterId_returnsResponse() {
        IndexesByMatterIdRequest request = IndexesByMatterIdRequest.builder().build();
        IndexesByMatterIdResponse response = new IndexesByMatterIdResponse();
        when(armApiClient.getIndexesByMatterId("Bearer token", request)).thenReturn(response);

        service.getIndexesByMatterId("Bearer token", request);

        verify(armApiClient).getIndexesByMatterId("Bearer token", request);
    }

    @Test
    void saveBackgroundSearch_returnsResponse() {
        SaveBackgroundSearchRequest request = SaveBackgroundSearchRequest.builder().build();
        SaveBackgroundSearchResponse response = new SaveBackgroundSearchResponse();
        when(armApiClient.saveBackgroundSearch("Bearer token", request)).thenReturn(response);

        service.saveBackgroundSearch("Bearer token", request);

        verify(armApiClient).saveBackgroundSearch("Bearer token", request);
    }

    @Test
    void getExtendedSearchesByMatter_returnsResponse() {
        ExtendedSearchesByMatterResponse response = createExtendedSearchesByMatterResponse();
        when(armApiClient.getExtendedSearchesByMatter("Bearer token", "body")).thenReturn(response);

        service.getExtendedSearchesByMatter("Bearer token", "body");

        verify(armApiClient).getExtendedSearchesByMatter("Bearer token", "body");

    }

    @Test
    void getProductionOutputFiles_returnsResponse() {
        ProductionOutputFilesRequest request = ProductionOutputFilesRequest.builder().build();
        ProductionOutputFilesResponse response = new ProductionOutputFilesResponse();
        when(armApiClient.getProductionOutputFiles("Bearer token", request)).thenReturn(response);

        service.getProductionOutputFiles("Bearer token", request);

        verify(armApiClient).getProductionOutputFiles("Bearer token", request);

    }

    @Test
    void getExtendedProductionsByMatter_returnsResponse() {

        service.getExtendedProductionsByMatter("Bearer token", "body");
        verify(armApiClient).getExtendedProductionsByMatter("Bearer token", "body");

    }

    @Test
    void downloadProduction_returnsResponse() {

        service.downloadProduction("Bearer token", "productionExportFileId");
        verify(armApiClient).downloadProduction("Bearer token", "productionExportFileId");
    }

    @Test
    void downloadProductionStub_returnsResponse() {
        service.downloadProduction("Bearer token", "eodIds", "productionExportFileId");
        verify(armApiClient).downloadProduction("Bearer token", "eodIds", "productionExportFileId");
    }

    @Test
    void availableEntitlementProfiles_returnsResponse() {
        service.availableEntitlementProfiles("Bearer token", EmptyRpoRequest.builder().build());
        verify(armApiClient).availableEntitlementProfiles("Bearer token", EmptyRpoRequest.builder().build());
    }

    @Test
    void selectEntitlementProfile_returnsResponse() {
        EmptyRpoRequest request = EmptyRpoRequest.builder().build();
        service.selectEntitlementProfile("Bearer token", "profileId", request);
        verify(armApiClient).selectEntitlementProfile("Bearer token", "profileId", request);
    }

    private CreateExportBasedOnSearchResultsTableRequest createRequestForCreateExportBasedOnSearchResultsTable(
        List<MasterIndexFieldByRecordClassSchema> headerColumns, String searchId, int searchItemsCount, String productionName,
        String storageAccountId) {

        return CreateExportBasedOnSearchResultsTableRequest.builder()
            .core(null)
            .formFields(null)
            .searchId(searchId)
            .searchitemsCount(searchItemsCount)
            .headerColumns(createHeaderColumnsFromMasterIndexFieldByRecordClassSchemaResponse(headerColumns))
            .productionName(productionName)
            .storageAccountId(storageAccountId)
            .onlyForCurrentUser(FALSE)
            .exportType(32)
            .build();
    }

    private List<CreateExportBasedOnSearchResultsTableRequest.HeaderColumn> createHeaderColumnsFromMasterIndexFieldByRecordClassSchemaResponse(
        List<MasterIndexFieldByRecordClassSchema> masterIndexFieldByRecordClassSchemas) {

        List<CreateExportBasedOnSearchResultsTableRequest.HeaderColumn> headerColumnList = new ArrayList<>();
        for (MasterIndexFieldByRecordClassSchema masterIndexField : masterIndexFieldByRecordClassSchemas) {
            headerColumnList.add(createHeaderColumn(masterIndexField));
        }
        return headerColumnList;
    }

    private CreateExportBasedOnSearchResultsTableRequest.HeaderColumn createHeaderColumn(
        MasterIndexFieldByRecordClassSchema masterIndexField) {
        return CreateExportBasedOnSearchResultsTableRequest.HeaderColumn.builder()
            .masterIndexField(masterIndexField.getMasterIndexField())
            .displayName(masterIndexField.getDisplayName())
            .propertyName(masterIndexField.getPropertyName())
            .propertyType(masterIndexField.getPropertyType())
            .isMasked(masterIndexField.getIsMasked())
            .build();
    }

    private MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchemaResponse(String propertyName1,
                                                                                                       String propertyName2) {
        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField1 = new MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField();
        masterIndexField1.setMasterIndexFieldId("1");
        masterIndexField1.setDisplayName("displayName");
        masterIndexField1.setPropertyName(propertyName1);
        masterIndexField1.setPropertyType("propertyType");
        masterIndexField1.setIsMasked(true);

        MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField masterIndexField2 = new MasterIndexFieldByRecordClassSchemaResponse.MasterIndexField();
        masterIndexField2.setMasterIndexFieldId("2");
        masterIndexField2.setDisplayName("displayName");
        masterIndexField2.setPropertyName(propertyName2);
        masterIndexField2.setPropertyType("propertyType");
        masterIndexField2.setIsMasked(false);

        MasterIndexFieldByRecordClassSchemaResponse response = new MasterIndexFieldByRecordClassSchemaResponse();
        response.setMasterIndexFields(List.of(masterIndexField1, masterIndexField2));
        return response;
    }

    private ExtendedSearchesByMatterResponse createExtendedSearchesByMatterResponse() {
        ExtendedSearchesByMatterResponse extendedSearchesByMatterResponse = new ExtendedSearchesByMatterResponse();
        extendedSearchesByMatterResponse.setStatus(200);
        extendedSearchesByMatterResponse.setIsError(false);
        ExtendedSearchesByMatterResponse.Search search = new ExtendedSearchesByMatterResponse.Search();
        search.setSearchId("SEARCH_ID");
        search.setTotalCount(4);
        search.setName("PRODUCTION_NAME");
        search.setIsSaved(true);
        ExtendedSearchesByMatterResponse.SearchDetail searchDetail = new ExtendedSearchesByMatterResponse.SearchDetail();
        searchDetail.setSearch(search);
        extendedSearchesByMatterResponse.setSearches(List.of(searchDetail));
        return extendedSearchesByMatterResponse;
    }
}