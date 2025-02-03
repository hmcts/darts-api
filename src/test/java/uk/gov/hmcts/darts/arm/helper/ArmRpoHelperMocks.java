package uk.gov.hmcts.darts.arm.helper;

import lombok.Getter;
import lombok.SneakyThrows;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;

import static org.mockito.Mockito.lenient;

@SuppressWarnings("PMD.NcssCount")
@Getter
public class ArmRpoHelperMocks {

    @Mock
    private ArmRpoStatusEntity inProgressRpoStatus;
    @Mock
    private ArmRpoStatusEntity completedRpoStatus;
    @Mock
    private ArmRpoStatusEntity failedRpoStatus;

    @Mock
    private ArmRpoStateEntity getRecordManagementMatterRpoState;
    @Mock
    private ArmRpoStateEntity getIndexesByMatterIdRpoState;
    @Mock
    private ArmRpoStateEntity getStorageAccountsRpoState;
    @Mock
    private ArmRpoStateEntity getProfileEntitlementsRpoState;
    @Mock
    private ArmRpoStateEntity getMasterIndexFieldByRecordClassSchemaPrimaryRpoState;
    @Mock
    private ArmRpoStateEntity addAsyncSearchRpoState;
    @Mock
    private ArmRpoStateEntity saveBackgroundSearchRpoState;
    @Mock
    private ArmRpoStateEntity getExtendedSearchesByMatterRpoState;
    @Mock
    private ArmRpoStateEntity getMasterIndexFieldByRecordClassSchemaSecondaryRpoState;
    @Mock
    private ArmRpoStateEntity createExportBasedOnSearchResultsTableRpoState;
    @Mock
    private ArmRpoStateEntity getExtendedProductionsByMatterRpoState;
    @Mock
    private ArmRpoStateEntity getProductionOutputFilesRpoState;
    @Mock
    private ArmRpoStateEntity downloadProductionRpoState;
    @Mock
    private ArmRpoStateEntity removeProductionRpoState = new ArmRpoStateEntity();

    private MockedStatic<ArmRpoHelper> armRpoHelperMockedStatic;
    private AutoCloseable closeable;

    public ArmRpoHelperMocks() {
        mockArmRpoHelper();
    }

    public final void mockArmRpoHelper() {
        closeable = MockitoAnnotations.openMocks(this);

        armRpoHelperMockedStatic = Mockito.mockStatic(ArmRpoHelper.class);

        inProgressRpoStatus = new ArmRpoStatusEntity();
        inProgressRpoStatus.setId(1);
        inProgressRpoStatus.setDescription("IN_PROGRESS");
        lenient().when(ArmRpoHelper.inProgressRpoStatus()).thenReturn(inProgressRpoStatus);

        completedRpoStatus = new ArmRpoStatusEntity();
        completedRpoStatus.setId(2);
        completedRpoStatus.setDescription("COMPLETED");
        lenient().when(ArmRpoHelper.completedRpoStatus()).thenReturn(completedRpoStatus);

        failedRpoStatus = new ArmRpoStatusEntity();
        failedRpoStatus.setId(3);
        failedRpoStatus.setDescription("FAILED");
        lenient().when(ArmRpoHelper.failedRpoStatus()).thenReturn(failedRpoStatus);

        getRecordManagementMatterRpoState = new ArmRpoStateEntity();
        getRecordManagementMatterRpoState.setId(1);
        getRecordManagementMatterRpoState.setDescription("GET_RECORD_MANAGEMENT_MATTER");
        lenient().when(ArmRpoHelper.getRecordManagementMatterRpoState()).thenReturn(getRecordManagementMatterRpoState);

        getIndexesByMatterIdRpoState = new ArmRpoStateEntity();
        getIndexesByMatterIdRpoState.setId(2);
        getIndexesByMatterIdRpoState.setDescription("GET_INDEXES_BY_MATTERID");
        lenient().when(ArmRpoHelper.getIndexesByMatterIdRpoState()).thenReturn(getIndexesByMatterIdRpoState);

        getStorageAccountsRpoState = new ArmRpoStateEntity();
        getStorageAccountsRpoState.setId(3);
        getStorageAccountsRpoState.setDescription("GET_STORAGE_ACCOUNTS");
        lenient().when(ArmRpoHelper.getStorageAccountsRpoState()).thenReturn(getStorageAccountsRpoState);

        getProfileEntitlementsRpoState = new ArmRpoStateEntity();
        getProfileEntitlementsRpoState.setId(4);
        getProfileEntitlementsRpoState.setDescription("GET_PROFILE_ENTITLEMENTS");
        lenient().when(ArmRpoHelper.getProfileEntitlementsRpoState()).thenReturn(getProfileEntitlementsRpoState);

        getMasterIndexFieldByRecordClassSchemaPrimaryRpoState = new ArmRpoStateEntity();
        getMasterIndexFieldByRecordClassSchemaPrimaryRpoState.setId(5);
        getMasterIndexFieldByRecordClassSchemaPrimaryRpoState.setDescription("GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY");
        lenient().when(ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaPrimaryRpoState()).thenReturn(getMasterIndexFieldByRecordClassSchemaPrimaryRpoState);

        addAsyncSearchRpoState = new ArmRpoStateEntity();
        addAsyncSearchRpoState.setId(6);
        addAsyncSearchRpoState.setDescription("ADD_ASYNC_SEARCH");
        lenient().when(ArmRpoHelper.addAsyncSearchRpoState()).thenReturn(addAsyncSearchRpoState);

        saveBackgroundSearchRpoState = new ArmRpoStateEntity();
        saveBackgroundSearchRpoState.setId(7);
        saveBackgroundSearchRpoState.setDescription("SAVE_BACKGROUND_SEARCH");
        lenient().when(ArmRpoHelper.saveBackgroundSearchRpoState()).thenReturn(saveBackgroundSearchRpoState);

        getExtendedSearchesByMatterRpoState = new ArmRpoStateEntity();
        getExtendedSearchesByMatterRpoState.setId(8);
        getExtendedSearchesByMatterRpoState.setDescription("GET_EXTENDED_SEARCHES_BY_MATTER");
        lenient().when(ArmRpoHelper.getExtendedSearchesByMatterRpoState()).thenReturn(getExtendedSearchesByMatterRpoState);

        getMasterIndexFieldByRecordClassSchemaSecondaryRpoState = new ArmRpoStateEntity();
        getMasterIndexFieldByRecordClassSchemaSecondaryRpoState.setId(9);
        getMasterIndexFieldByRecordClassSchemaSecondaryRpoState.setDescription("GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_SECONDARY");
        lenient().when(ArmRpoHelper.getMasterIndexFieldByRecordClassSchemaSecondaryRpoState()).thenReturn(
            getMasterIndexFieldByRecordClassSchemaSecondaryRpoState);

        createExportBasedOnSearchResultsTableRpoState = new ArmRpoStateEntity();
        createExportBasedOnSearchResultsTableRpoState.setId(10);
        createExportBasedOnSearchResultsTableRpoState.setDescription("CREATE_EXPORT_BASED_ON_SEARCH_RESULTS_TABLE");
        lenient().when(ArmRpoHelper.createExportBasedOnSearchResultsTableRpoState()).thenReturn(createExportBasedOnSearchResultsTableRpoState);

        getExtendedProductionsByMatterRpoState = new ArmRpoStateEntity();
        getExtendedProductionsByMatterRpoState.setId(11);
        getExtendedProductionsByMatterRpoState.setDescription("GET_EXTENDED_PRODUCTIONS_BY_MATTER");
        lenient().when(ArmRpoHelper.getExtendedProductionsByMatterRpoState()).thenReturn(getExtendedProductionsByMatterRpoState);

        getProductionOutputFilesRpoState = new ArmRpoStateEntity();
        getProductionOutputFilesRpoState.setId(12);
        getProductionOutputFilesRpoState.setDescription("GET_PRODUCTION_OUTPUT_FILES");
        lenient().when(ArmRpoHelper.getProductionOutputFilesRpoState()).thenReturn(getProductionOutputFilesRpoState);

        downloadProductionRpoState = new ArmRpoStateEntity();
        downloadProductionRpoState.setId(13);
        downloadProductionRpoState.setDescription("DOWNLOAD_PRODUCTION");
        lenient().when(ArmRpoHelper.downloadProductionRpoState()).thenReturn(downloadProductionRpoState);

        removeProductionRpoState = new ArmRpoStateEntity();
        removeProductionRpoState.setId(14);
        removeProductionRpoState.setDescription("REMOVE_PRODUCTION");
        lenient().when(ArmRpoHelper.removeProductionRpoState()).thenReturn(removeProductionRpoState);

    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.ExhaustiveSwitchHasDefault"})
    public ArmRpoStateEntity armRpoStateEnumToEntity(ArmRpoStateEnum armRpoStateEnum) {
        switch (armRpoStateEnum) {
            case GET_RECORD_MANAGEMENT_MATTER:
                return getRecordManagementMatterRpoState;
            case GET_INDEXES_BY_MATTERID:
                return getIndexesByMatterIdRpoState;
            case GET_STORAGE_ACCOUNTS:
                return getStorageAccountsRpoState;
            case GET_PROFILE_ENTITLEMENTS:
                return getProfileEntitlementsRpoState;
            case GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY:
                return getMasterIndexFieldByRecordClassSchemaPrimaryRpoState;
            case ADD_ASYNC_SEARCH:
                return addAsyncSearchRpoState;
            case SAVE_BACKGROUND_SEARCH:
                return saveBackgroundSearchRpoState;
            case GET_EXTENDED_SEARCHES_BY_MATTER:
                return getExtendedSearchesByMatterRpoState;
            case GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_SECONDARY:
                return getMasterIndexFieldByRecordClassSchemaSecondaryRpoState;
            case CREATE_EXPORT_BASED_ON_SEARCH_RESULTS_TABLE:
                return createExportBasedOnSearchResultsTableRpoState;
            case GET_EXTENDED_PRODUCTIONS_BY_MATTER:
                return getExtendedProductionsByMatterRpoState;
            case GET_PRODUCTION_OUTPUT_FILES:
                return getProductionOutputFilesRpoState;
            case DOWNLOAD_PRODUCTION:
                return downloadProductionRpoState;
            case REMOVE_PRODUCTION:
                return removeProductionRpoState;
            default:
                throw new IllegalArgumentException("Unknown ArmRpoStateEnum: " + armRpoStateEnum);
        }
    }

    @SneakyThrows
    public void close() {
        armRpoHelperMockedStatic.close();
        closeable.close();
    }
}
