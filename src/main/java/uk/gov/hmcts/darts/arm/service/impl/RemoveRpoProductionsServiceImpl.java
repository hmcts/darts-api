package uk.gov.hmcts.darts.arm.service.impl;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.service.RemoveRpoProductionsService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List; 

@Service
@AllArgsConstructor
@Slf4j
public class RemoveRpoProductionsServiceImpl implements RemoveRpoProductionsService {


    private final LogApi logApi;
    private final UserIdentity userIdentity;
    private final ArmRpoService armRpoService;
    private final ArmRpoUtil armRpoUtil;
    private final ArmRpoApi armRpoApi;


    @Override
    public void removeOldArmRpoProductions(boolean isManualRun, Duration waitDuration, int batchSize) {
        log.info("Removing ARM RPO productions - isManualRun: {}, duration: {}, batchSize: {}",
                 isManualRun, waitDuration, batchSize);
        List<Integer> ardIdsToRemove;
        try {
            log.info("Finding ARM RPO executions with status FAILED older than: {}", waitDuration);
            ardIdsToRemove = armRpoService.findIdsByStatusAndLastModifiedDateTimeAfter(
                ArmRpoHelper.failedRpoStatus(), OffsetDateTime.now().minus(waitDuration)
            );
            if (ardIdsToRemove.isEmpty()) {
                log.info("No ARM RPO productions found to remove older than: {}", waitDuration);
                return;
            }
            
        } catch (Exception e) {
            log.warn("Exception occurred while preparing to remove ARM RPO productions: {}", e.getMessage(), e);
            logApi.removeOldArmRpoProductionsFailed();
            return;
        }
        removeProductionsBatch(ardIdsToRemove, userIdentity.getUserAccount());
    }
    
    private void removeProductionsBatch(List<Integer> ardIds, UserAccountEntity userAccount) {
        for (Integer ardId : ardIds) {
            try {
                log.info("Removing ARM RPO production for ard_id: {}", ardId);
                armRpoApi.removeProduction(armRpoUtil.getBearerToken("removeProduction"), ardId, userAccount);
                logApi.removeOldArmRpoProductionsSuccessful(ardId);
            } catch (FeignException feignException) {
                int status = feignException.status();
                // If unauthorized or forbidden, retry once with a refreshed token
                if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                    try {
                        String refreshedBearer = armRpoUtil.retryGetBearerToken("removeProduction");
                        armRpoApi.removeProduction(refreshedBearer, ardId, userAccount);
                    } catch (FeignException retryEx) {
                        logApi.removeOldArmRpoProductionsFailed(ardId);
                    }
                }
            } catch (Exception ex) {
                log.error("Error while removing old RPO production", ex);
                logApi.removeOldArmRpoProductionsFailed(ardId);
            }
        }
    }
    
}
