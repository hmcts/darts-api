package uk.gov.hmcts.darts.arm.service.impl;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.RemoveRpoProductionsService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum;
import uk.gov.hmcts.darts.common.repository.ArmRpoExecutionDetailRepository;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.darts.common.enums.ArmRpoStatusEnum.FAILED;

@Service
@AllArgsConstructor
@Slf4j
public class RemoveRpoProductionsServiceImpl implements RemoveRpoProductionsService {

    private final ArmRpoExecutionDetailRepository armRpoExecutionDetailRepository;
    private final ArmApiService armApiService;
    private final LogApi logApi;
    private final ArmRpoApi armRpoApi;
    private final UserIdentity userIdentity;


    @SneakyThrows
    @Override
    public void removeOldArmRpoProductions(boolean isManualRun, Duration duration, int batchSize) {
        log.info("Removing ARM RPO productions - isManualRun: {}, duration: {}, batchSize: {}",
                 isManualRun, duration, batchSize);
        List<Integer> ardIdsToRemove;
        String bearerToken;
        UserAccountEntity userAccount;
        try {
            List<ArmRpoExecutionDetailEntity> allArdIds = armRpoExecutionDetailRepository.findAll();
            for (var ard : allArdIds) {
                log.debug("ARM RPO Execution Detail: ard_id={}, status={}, lastModified={}",
                          ard.getId(),
                          ard.getArmRpoStatus().getDescription(),
                          ard.getLastModifiedDateTime());
            }
            ardIdsToRemove = armRpoExecutionDetailRepository.findIdsByStatusAndLastModifiedDateTimeAfter(
                statusOf(FAILED), OffsetDateTime.now().minus(duration)
            );
            if (ardIdsToRemove.isEmpty()) {
                log.info("No ARM RPO productions found to remove older than: {}", duration);
                return;
            }
            // get bearerToken
            // TODO integrate bearer token caching from DMP-5303
            bearerToken = armApiService.getArmBearerToken();
            if (isNull(bearerToken)) {
                log.warn("Unable to get bearer token to poll ARM RPO");
                logApi.removeOldArmRpoProductionsFailed();
                return;
            }
            userAccount = userIdentity.getUserAccount();
        } catch (Exception e) {
            log.warn("Exception occurred while preparing to remove ARM RPO productions: {}", e.getMessage(), e);
            logApi.removeOldArmRpoProductionsFailed();
            return;
        }

        for (Integer ardId : ardIdsToRemove) {
            try {
                log.info("Removing ARM RPO production for ard_id: {}", ardId);
                armRpoApi.removeProduction(bearerToken, ardId, userAccount);
                logApi.removeOldArmRpoProductionsSuccessful(ardId);
            } catch (Exception e) {
                logApi.removeOldArmRpoProductionsFailed(ardId);
            }
            
        }
    }
    
    private ArmRpoStatusEntity statusOf(ArmRpoStatusEnum status) {
        var armRpoStatusEntity = new ArmRpoStatusEntity();
        armRpoStatusEntity.setId(status.getId());
        armRpoStatusEntity.setDescription(status.name());
        return armRpoStatusEntity;
    }
    
}
