package uk.gov.hmcts.darts.arm.component;

import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ArmAutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RPO_PENDING;
import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.ARM_RPO_POLL_TASK_NAME;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getArmRpoExecutionDetailTestData;

@TestPropertySource(properties = {"darts.storage.arm.is_mock_arm_rpo_download_csv=true"})
@SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.CloseResource"})
class StubbedArmRpoDownloadProductionIntTest extends IntegrationBase {
    @Autowired
    private ArmRpoDownloadProduction stubbedArmRpoDownloadProduction;

    @Autowired
    private CurrentTimeHelper currentTimeHelper;

    @Autowired
    private ExternalObjectDirectoryStub externalObjectDirectoryStub;

    @Autowired
    private AutomatedTaskRepository automatedTaskRepository;

    @Autowired
    private ArmAutomatedTaskRepository armAutomatedTaskRepository;


    @MockBean
    private ArmRpoClient armRpoClient;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private UserAccountEntity userAccountEntity;

    @BeforeEach
    void setUp() {

        userAccountEntity = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        armRpoExecutionDetailEntity = dartsPersistence.save(getArmRpoExecutionDetailTestData().minimalArmRpoExecutionDetailEntity());
    }

    @Test
    void downloadProduction_shouldThrowException_whenAutomatedTaskNotFound() {
        // when
        ArmRpoException exception = assertThrows(ArmRpoException.class, () ->
            stubbedArmRpoDownloadProduction.downloadProduction("token", 1, "fileId"));

        // then
        assertTrue(exception.getMessage().contains("unable to find arm automated task"));
    }

    @Test
    void downloadProduction_shouldThrowException_whenNoEodsFound() {
        ArmRpoException exception = assertThrows(ArmRpoException.class, () ->
            stubbedArmRpoDownloadProduction.downloadProduction("token", 1, "fileId"));

        assertTrue(exception.getMessage().contains("no eods found"));
    }

    @Test
    void downloadProduction_shouldReturnResponse_whenEodsFound() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        // given
        OffsetDateTime now = currentTimeHelper.currentOffsetDateTime();
        OffsetDateTime pastCurrentDateTime = now.minusHours(30);

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndMediaLocation(
            ExternalLocationTypeEnum.ARM, ARM_RPO_PENDING, 20, Optional.of(pastCurrentDateTime));

        OffsetDateTime ingestionStartDateTime = currentTimeHelper.currentOffsetDateTime().minusHours(30);
        externalObjectDirectoryEntities.forEach(eod -> {
            if (eod.getId() % 2 == 0 && (eod.getId() % 3 != 0)) {
                // within the time range
                eod.setCreatedDateTime(ingestionStartDateTime);
                eod.setDataIngestionTs(currentTimeHelper.currentOffsetDateTime().minusHours(26));
            } else if (eod.getId() % 3 == 0) {
                // before the time range
                eod.setCreatedDateTime(currentTimeHelper.currentOffsetDateTime().minusHours(40));
                eod.setDataIngestionTs(currentTimeHelper.currentOffsetDateTime().minusHours(31));
            } else {
                // after the time range
                eod.setCreatedDateTime(currentTimeHelper.currentOffsetDateTime().minusHours(15));
                eod.setDataIngestionTs(currentTimeHelper.currentOffsetDateTime().minusHours(10));
            }
        });
        dartsPersistence.saveAll(externalObjectDirectoryEntities);

        List<AutomatedTaskEntity> automatedTasks = automatedTaskRepository.findAll();

        for (AutomatedTaskEntity automatedTask : automatedTasks) {
            var armAutomatedTask = new ArmAutomatedTaskEntity();
            armAutomatedTask.setAutomatedTask(automatedTask);
            if (ARM_RPO_POLL_TASK_NAME.getTaskName().equals(automatedTask.getTaskName())) {
                armAutomatedTask.setRpoCsvStartHour(25);
                armAutomatedTask.setRpoCsvEndHour(49);
            }
            armAutomatedTaskRepository.save(armAutomatedTask);
        }

        Response response = mock(Response.class);
        when(armRpoClient.downloadProduction(anyString(), anyString(), anyString()))
            .thenReturn(response);

        // when
        Response result = stubbedArmRpoDownloadProduction.downloadProduction("token", 1, "fileId");

        // then
        assertNotNull(result);
        verify(armRpoClient).downloadProduction(anyString(), anyString(), anyString());
    }
}
