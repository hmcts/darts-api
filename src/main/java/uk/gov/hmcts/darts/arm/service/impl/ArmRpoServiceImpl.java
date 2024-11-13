package uk.gov.hmcts.darts.arm.service.impl;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ArmAutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.ArmRpoExecutionDetailRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.util.CsvFileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

@Service
@AllArgsConstructor
@Slf4j
public class ArmRpoServiceImpl implements ArmRpoService {

    public static final String ARM_RPO_EXECUTION_DETAIL_NOT_FOUND = "ArmRpoExecutionDetail not found";
    private static final String ADD_ASYNC_SEARCH_RELATED_TASK_NAME = "ProcessE2EArmRpoPending";
    private static final String CLIENT_IDENTIFIER_CSV_HEADER = "Client Identifier";

    private final ArmRpoExecutionDetailRepository armRpoExecutionDetailRepository;
    private final EntityManager entityManager;
    private final ArmAutomatedTaskRepository armAutomatedTaskRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Override
    @Transactional
    public ArmRpoExecutionDetailEntity createArmRpoExecutionDetailEntity(UserAccountEntity userAccount) {
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();

        UserAccountEntity mergedUserAccountEntity = entityManager.merge(userAccount);
        armRpoExecutionDetailEntity.setCreatedBy(mergedUserAccountEntity);
        armRpoExecutionDetailEntity.setLastModifiedBy(mergedUserAccountEntity);

        return saveArmRpoExecutionDetailEntity(armRpoExecutionDetailEntity);
    }

    @Override
    public ArmRpoExecutionDetailEntity getArmRpoExecutionDetailEntity(Integer executionId) {
        return armRpoExecutionDetailRepository.findById(executionId).orElseThrow(() -> new DartsException(ARM_RPO_EXECUTION_DETAIL_NOT_FOUND));
    }

    public ArmRpoExecutionDetailEntity getLatestArmRpoExecutionDetailEntity() {
        return armRpoExecutionDetailRepository.findTopOrderByCreatedDateTimeDesc();
    }

    @Override
    public void updateArmRpoStateAndStatus(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity, ArmRpoStateEntity armRpoStateEntity,
                                           ArmRpoStatusEntity armRpoStatusEntity, UserAccountEntity userAccountEntity) {
        String previousState = nonNull(armRpoExecutionDetailEntity.getArmRpoState()) ? armRpoExecutionDetailEntity.getArmRpoState().getDescription() : null;
        log.debug("Setting execution detail {} state from {} to {}", armRpoExecutionDetailEntity.getId(),
                  previousState,
                  armRpoStateEntity.getDescription());
        armRpoExecutionDetailEntity.setArmRpoState(armRpoStateEntity);
        updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoStatusEntity, userAccountEntity);
    }

    @Override
    public void updateArmRpoStatus(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity, ArmRpoStatusEntity armRpoStatusEntity,
                                   UserAccountEntity userAccountEntity) {
        String previousStatus = nonNull(armRpoExecutionDetailEntity.getArmRpoStatus()) ? armRpoExecutionDetailEntity.getArmRpoStatus().getDescription() : null;
        log.debug("Setting execution detail {} status from {} to {}", armRpoExecutionDetailEntity.getId(),
                  previousStatus,
                  armRpoStatusEntity.getDescription());
        armRpoExecutionDetailEntity.setArmRpoStatus(armRpoStatusEntity);
        armRpoExecutionDetailEntity.setLastModifiedBy(userAccountEntity);
        saveArmRpoExecutionDetailEntity(armRpoExecutionDetailEntity);
    }

    @Override
    public ArmRpoExecutionDetailEntity saveArmRpoExecutionDetailEntity(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        return armRpoExecutionDetailRepository.save(armRpoExecutionDetailEntity);
    }

    @Override
    public void reconcileArmRpoCsvData(ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity, List<File> csvFiles) {
        ObjectRecordStatusEntity armRpoPending = EodHelper.armRpoPendingStatus();
        StringBuilder errorMessage = new StringBuilder("Failure during ARM RPO CSV Reconciliation: ");

        ArmAutomatedTaskEntity armAutomatedTaskEntity = armAutomatedTaskRepository.findByAutomatedTask_taskName(ADD_ASYNC_SEARCH_RELATED_TASK_NAME)
            .orElseThrow(() -> new ArmRpoException(errorMessage.append("Automated task ProcessE2EArmRpoPending not found.").toString()));

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = externalObjectDirectoryRepository.findByStatusAndIngestionDate(
            armRpoPending,
            armRpoExecutionDetailEntity.getCreatedDateTime().minusHours(armAutomatedTaskEntity.getRpoCsvEndHour()),
            armRpoExecutionDetailEntity.getCreatedDateTime().minusHours(armAutomatedTaskEntity.getRpoCsvStartHour()));

        List<Integer> csvEodList = new ArrayList<>();
        for (File csvFile : csvFiles) {
            try (Reader reader = new FileReader(csvFile.getPath())) {
                Iterable<CSVRecord> records = CsvFileUtil.readCsv(reader);

                for (CSVRecord csvRecord : records) {
                    String csvEod = csvRecord.get(CLIENT_IDENTIFIER_CSV_HEADER);
                    if (StringUtils.isNotBlank(csvEod)) {
                        csvEodList.add(Integer.parseInt(csvEod));
                    }
                }
            } catch (FileNotFoundException e) {
                log.error(errorMessage.append("Unable to find CSV file for Reconciliation ").toString(), e);
                throw new ArmRpoException(errorMessage.toString());
            } catch (Exception e) {
                log.error(errorMessage.toString(), e.getMessage());
                throw new ArmRpoException(errorMessage.toString());
            }
        }

        externalObjectDirectoryEntities.forEach(
            externalObjectDirectoryEntity -> {
                if (csvEodList.contains(externalObjectDirectoryEntity.getId())) {
                    externalObjectDirectoryEntity.setStatus(EodHelper.storedStatus());
                } else {
                    externalObjectDirectoryEntity.setStatus(EodHelper.armReplayStatus());
                }
            }
        );
        externalObjectDirectoryRepository.saveAllAndFlush(externalObjectDirectoryEntities);
    }
}
