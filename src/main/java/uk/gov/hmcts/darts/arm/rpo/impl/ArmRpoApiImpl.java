package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ArmAsyncSearchResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedProductionsByMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedSearchesByMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.IndexesByMatterIdResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.MasterIndexFieldByRecordClassSchemaResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.ProfileEntitlementResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.RecordManagementMatterResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.StorageAccountResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.io.InputStream;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ArmRpoApiImpl implements ArmRpoApi {

    public static final String ARM_GET_RECORD_MANAGEMENT_MATTER_ERROR = "Error during ARM get record management matter";
    private final ArmRpoClient armRpoClient;
    private final ArmRpoService armRpoService;

    @Override
    public void getRecordManagementMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        var armRpoExecutionDetailEntity = armRpoService.getArmRpoExecutionDetailEntity(executionId);
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, ArmRpoHelper.getRecordManagementMatterRpoState(),
                                                 ArmRpoHelper.inProgressRpoStatus(), userAccount);

        RecordManagementMatterResponse recordManagementMatterResponse;
        try {
            recordManagementMatterResponse = armRpoClient.getRecordManagementMatter(bearerToken);
        } catch (FeignException e) {
            // this ensures the full error body containing the ARM error detail is logged rather than a truncated version
            log.error("Error during ARM get record management matter: {}", e.contentUTF8());
            throw handleFailureAndCreateException(ARM_GET_RECORD_MANAGEMENT_MATTER_ERROR, armRpoExecutionDetailEntity, userAccount);
        }
        
        if (recordManagementMatterResponse == null
            || recordManagementMatterResponse.getRecordManagementMatter() == null
            || recordManagementMatterResponse.getRecordManagementMatter().getMatterId() == null) {
            throw handleFailureAndCreateException(ARM_GET_RECORD_MANAGEMENT_MATTER_ERROR, armRpoExecutionDetailEntity, userAccount);
        }

        armRpoExecutionDetailEntity.setMatterId(recordManagementMatterResponse.getRecordManagementMatter().getMatterId());
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.completedRpoStatus(), userAccount);
    }

    @Override
    public IndexesByMatterIdResponse getIndexesByMatterId(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public StorageAccountResponse getStorageAccounts(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public ProfileEntitlementResponse getProfileEntitlements(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public MasterIndexFieldByRecordClassSchemaResponse getMasterIndexFieldByRecordClassSchema(String bearerToken,
                                                                                              Integer executionId,
                                                                                              Integer rpoStageId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public ArmAsyncSearchResponse addAsyncSearch(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public void saveBackgroundSearch(String bearerToken, Integer executionId, String searchName, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public ExtendedSearchesByMatterResponse getExtendedSearchesByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public boolean createExportBasedOnSearchResultsTable(String bearerToken, Integer executionId,
                                                         List<MasterIndexFieldByRecordClassSchemaResponse> headerColumns, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public ExtendedProductionsByMatterResponse getExtendedProductionsByMatter(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public List<String> getProductionOutputFiles(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public InputStream downloadProduction(String bearerToken, Integer executionId, String productionExportFileID, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public void removeProduction(String bearerToken, Integer executionId, UserAccountEntity userAccount) {
        throw new NotImplementedException("Method not implemented");
    }

    private ArmRpoException handleFailureAndCreateException(String message,
                                                            ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity,
                                                            UserAccountEntity userAccount) {
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.failedRpoStatus(), userAccount);
        return new ArmRpoException(message);
    }
}
