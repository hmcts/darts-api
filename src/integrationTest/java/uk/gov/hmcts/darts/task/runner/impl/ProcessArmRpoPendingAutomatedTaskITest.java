package uk.gov.hmcts.darts.task.runner.impl;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("ProcessArmRpoPendingAutomatedTask test")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@TestPropertySource(properties = {
    "darts.automated.task.process-e2e-arm-rpo-pending.process-e2e-arm-rpo=false"
})
class ProcessArmRpoPendingAutomatedTaskITest extends PostgresIntegrationBase {
    private final ProcessArmRpoPendingAutomatedTask processArmRpoPendingAutomatedTask;
    private static final int AUTOMATION_USER_ID = -36;

    @Test
    void positiveCorrectStatusAndDataIngestionTime() {
        MediaEntity media = dartsDatabase.getMediaStub().createMediaEntity("Courthouse", "Courtroom", OffsetDateTime.now().minusHours(1),
                                                                           OffsetDateTime.now().plusHours(2), 1, "mediaType");
        ExternalObjectDirectoryEntity externalObjectDirectoryEntityOriginal = dartsDatabase.getExternalObjectDirectoryStub()
            .createExternalObjectDirectory(media, ObjectRecordStatusEnum.ARM_RPO_PENDING, ExternalLocationTypeEnum.ARM, UUID.randomUUID());
        externalObjectDirectoryEntityOriginal.setDataIngestionTs(OffsetDateTime.now().minusHours(1));
        dartsDatabase.save(externalObjectDirectoryEntityOriginal);

        transactionalUtil.executeInTransaction(() -> {
            assertThat(externalObjectDirectoryEntityOriginal.getStatus().getId()).isEqualTo(ObjectRecordStatusEnum.ARM_RPO_PENDING.getId());
        });

        processArmRpoPendingAutomatedTask.preRunTask();
        processArmRpoPendingAutomatedTask.runTask();

        transactionalUtil.executeInTransaction(() -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntityAfter = dartsDatabase.getExternalObjectDirectoryRepository().findById(
                externalObjectDirectoryEntityOriginal.getId()).orElseThrow();
            assertThat(externalObjectDirectoryEntityAfter.getStatus().getId())
                .isEqualTo(ObjectRecordStatusEnum.STORED.getId());
            assertThat(externalObjectDirectoryEntityAfter.getLastModifiedBy().getId())
                .isEqualTo(AUTOMATION_USER_ID);
            assertThat(externalObjectDirectoryEntityAfter.getLastModifiedDateTime())
                .isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES));
            assertThat(externalObjectDirectoryEntityAfter.getLastModifiedDateTime())
                .isNotEqualTo(externalObjectDirectoryEntityOriginal.getLastModifiedDateTime());
        });
    }

    @ParameterizedTest(name = "Status: {0}")
    @EnumSource(value = ObjectRecordStatusEnum.class, mode = EnumSource.Mode.EXCLUDE, names = {"ARM_RPO_PENDING", "STORED"})
    void positiveIncorrectStatusButCorrectDataIngestionTime(ObjectRecordStatusEnum status) {
        MediaEntity media = dartsDatabase.getMediaStub().createMediaEntity("Courthouse", "Courtroom", OffsetDateTime.now().minusHours(1),
                                                                           OffsetDateTime.now().plusHours(2), 1, "mediaType");
        ExternalObjectDirectoryEntity externalObjectDirectoryEntityOriginal = dartsDatabase.getExternalObjectDirectoryStub()
            .createExternalObjectDirectory(media, status, ExternalLocationTypeEnum.ARM, UUID.randomUUID());
        externalObjectDirectoryEntityOriginal.setDataIngestionTs(OffsetDateTime.now().minusHours(1));
        dartsDatabase.save(externalObjectDirectoryEntityOriginal);

        transactionalUtil.executeInTransaction(() -> {
            assertThat(externalObjectDirectoryEntityOriginal.getStatus().getId()).isEqualTo(status.getId());
        });

        processArmRpoPendingAutomatedTask.preRunTask();
        processArmRpoPendingAutomatedTask.runTask();

        transactionalUtil.executeInTransaction(() -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntityAfter = dartsDatabase.getExternalObjectDirectoryRepository().findById(
                externalObjectDirectoryEntityOriginal.getId()).orElseThrow();
            assertThat(externalObjectDirectoryEntityAfter.getStatus().getId()).isEqualTo(status.getId());
            assertThat(externalObjectDirectoryEntityAfter.getLastModifiedBy().getId()).isEqualTo(
                externalObjectDirectoryEntityOriginal.getLastModifiedBy().getId());
        });
    }

    @Test
    void positiveCorrectStatusButInCorrectDataIngestionTime() {
        MediaEntity media = dartsDatabase.getMediaStub().createMediaEntity("Courthouse", "Courtroom", OffsetDateTime.now().minusHours(1),
                                                                           OffsetDateTime.now().plusHours(2), 1, "mediaType");
        ExternalObjectDirectoryEntity externalObjectDirectoryEntityOriginal = dartsDatabase.getExternalObjectDirectoryStub()
            .createExternalObjectDirectory(media, ObjectRecordStatusEnum.ARM_RPO_PENDING, ExternalLocationTypeEnum.ARM, UUID.randomUUID());
        externalObjectDirectoryEntityOriginal.setDataIngestionTs(OffsetDateTime.now());
        dartsDatabase.save(externalObjectDirectoryEntityOriginal);

        transactionalUtil.executeInTransaction(() -> {
            assertThat(externalObjectDirectoryEntityOriginal.getStatus().getId()).isEqualTo(ObjectRecordStatusEnum.ARM_RPO_PENDING.getId());
        });

        processArmRpoPendingAutomatedTask.preRunTask();
        processArmRpoPendingAutomatedTask.runTask();

        transactionalUtil.executeInTransaction(() -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntityAfter = dartsDatabase.getExternalObjectDirectoryRepository().findById(
                externalObjectDirectoryEntityOriginal.getId()).orElseThrow();
            assertThat(externalObjectDirectoryEntityAfter.getStatus().getId()).isEqualTo(
                ObjectRecordStatusEnum.ARM_RPO_PENDING.getId());
            assertThat(externalObjectDirectoryEntityAfter.getLastModifiedBy().getId())
                .isEqualTo(externalObjectDirectoryEntityOriginal.getLastModifiedBy().getId());
        });
    }
}
