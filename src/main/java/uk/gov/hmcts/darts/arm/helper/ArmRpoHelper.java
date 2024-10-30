package uk.gov.hmcts.darts.arm.helper;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStateEnum;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.common.repository.ArmRpoStateRepository;
import uk.gov.hmcts.darts.common.repository.ArmRpoStatusRepository;

@Component
@Accessors(fluent = true)
@RequiredArgsConstructor
public class ArmRpoHelper {

    private final ArmRpoStateRepository armRpoStateRepository;
    private final ArmRpoStatusRepository armRpoStatusRepository;

    @Getter
    private static ArmRpoStatusEntity inProgressRpoStatus;
    @Getter
    private static ArmRpoStatusEntity completedRpoStatus;
    @Getter
    private static ArmRpoStatusEntity failedRpoStatus;

    @Getter
    private static ArmRpoStateEntity getRecordManagementMatterRpoState;
    @Getter
    private static ArmRpoStateEntity getIndexesByMatterIdRpoState;
    @Getter
    private static ArmRpoStateEntity getStorageAccountsRpoState;
    @Getter
    private static ArmRpoStateEntity getProfileEntitlementsRpoState;
    @Getter
    private static ArmRpoStateEntity getMasterIndexFieldByRecordClassSchemaPrimaryRpoState;
    @Getter
    private static ArmRpoStateEntity addAsyncSearchRpoState;
    @Getter
    private static ArmRpoStateEntity saveBackgroundSearchRpoState;
    @Getter
    private static ArmRpoStateEntity getExtendedSearchesByMatterRpoState;
    @Getter
    private static ArmRpoStateEntity getMasterIndexFieldByRecordClassSchemaSecondaryRpoState;
    @Getter
    private static ArmRpoStateEntity createExportBasedOnSearchResultsTableRpoState;
    @Getter
    private static ArmRpoStateEntity getExtendedProductionsByMatterRpoState;
    @Getter
    private static ArmRpoStateEntity getProductionOutputFilesRpoState;
    @Getter
    private static ArmRpoStateEntity downloadProductionRpoState;
    @Getter
    private static ArmRpoStateEntity removeProductionRpoState;

    @SuppressWarnings("java:S2696")
    @PostConstruct
    public void init() {
        inProgressRpoStatus = armRpoStatusRepository.findById(ArmRpoStatusEnum.IN_PROGRESS.getId()).orElseThrow();
        completedRpoStatus = armRpoStatusRepository.findById(ArmRpoStatusEnum.COMPLETED.getId()).orElseThrow();
        failedRpoStatus = armRpoStatusRepository.findById(ArmRpoStatusEnum.FAILED.getId()).orElseThrow();

        getRecordManagementMatterRpoState = armRpoStateRepository.findById(ArmRpoStateEnum.GET_RECORD_MANAGEMENT_MATTER.getId()).orElseThrow();
        getIndexesByMatterIdRpoState = armRpoStateRepository.findById(ArmRpoStateEnum.GET_INDEXES_BY_MATTERID.getId()).orElseThrow();
        getStorageAccountsRpoState = armRpoStateRepository.findById(ArmRpoStateEnum.GET_STORAGE_ACCOUNTS.getId()).orElseThrow();
        getProfileEntitlementsRpoState = armRpoStateRepository.findById(ArmRpoStateEnum.GET_PROFILE_ENTITLEMENTS.getId()).orElseThrow();
        getMasterIndexFieldByRecordClassSchemaPrimaryRpoState = armRpoStateRepository.findById(
            ArmRpoStateEnum.GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY.getId()).orElseThrow();
        addAsyncSearchRpoState = armRpoStateRepository.findById(ArmRpoStateEnum.ADD_ASYNC_SEARCH.getId()).orElseThrow();
        saveBackgroundSearchRpoState = armRpoStateRepository.findById(ArmRpoStateEnum.SAVE_BACKGROUND_SEARCH.getId()).orElseThrow();
        getExtendedSearchesByMatterRpoState = armRpoStateRepository.findById(ArmRpoStateEnum.GET_EXTENDED_SEARCHES_BY_MATTER.getId()).orElseThrow();
        getMasterIndexFieldByRecordClassSchemaSecondaryRpoState = armRpoStateRepository.findById(
            ArmRpoStateEnum.GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_SECONDARY.getId()).orElseThrow();
        createExportBasedOnSearchResultsTableRpoState = armRpoStateRepository.findById(
            ArmRpoStateEnum.CREATE_EXPORT_BASED_ON_SEARCH_RESULTS_TABLE.getId()).orElseThrow();
        getExtendedProductionsByMatterRpoState = armRpoStateRepository.findById(ArmRpoStateEnum.GET_EXTENDED_PRODUCTIONS_BY_MATTER.getId()).orElseThrow();
        getProductionOutputFilesRpoState = armRpoStateRepository.findById(ArmRpoStateEnum.GET_PRODUCTION_OUTPUT_FILES.getId()).orElseThrow();
        downloadProductionRpoState = armRpoStateRepository.findById(ArmRpoStateEnum.DOWNLOAD_PRODUCTION.getId()).orElseThrow();
        removeProductionRpoState = armRpoStateRepository.findById(ArmRpoStateEnum.REMOVE_PRODUCTION.getId()).orElseThrow();
    }

    public static boolean isEqual(ArmRpoStatusEntity rpoStatus1, ArmRpoStatusEntity rpoStatus2) {
        return rpoStatus1.getId().equals(rpoStatus2.getId());
    }

    public static boolean isEqual(ArmRpoStateEntity rpoState1, ArmRpoStateEntity rpoState2) {
        return rpoState1.getId().equals(rpoState2.getId());
    }

}
