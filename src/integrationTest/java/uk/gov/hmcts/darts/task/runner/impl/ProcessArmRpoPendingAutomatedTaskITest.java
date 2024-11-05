package uk.gov.hmcts.darts.task.runner.impl;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("ProcessArmRpoPendingAutomatedTask test")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ProcessArmRpoPendingAutomatedTaskITest extends PostgresIntegrationBase {
    private final ProcessArmRpoPendingAutomatedTask processArmRpoPendingAutomatedTask;

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

        processArmRpoPendingAutomatedTask.runTask();

        transactionalUtil.executeInTransaction(() -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntityAfter = dartsDatabase.getExternalObjectDirectoryRepository().findById(
                externalObjectDirectoryEntityOriginal.getId()).orElseThrow();
            assertThat(externalObjectDirectoryEntityAfter.getStatus().getId()).isEqualTo(
                ObjectRecordStatusEnum.STORED.getId());
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

        processArmRpoPendingAutomatedTask.runTask();

        transactionalUtil.executeInTransaction(() -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntityAfter = dartsDatabase.getExternalObjectDirectoryRepository().findById(
                externalObjectDirectoryEntityOriginal.getId()).orElseThrow();
            assertThat(externalObjectDirectoryEntityAfter.getStatus().getId()).isEqualTo(status.getId());
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

        processArmRpoPendingAutomatedTask.runTask();

        transactionalUtil.executeInTransaction(() -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntityAfter = dartsDatabase.getExternalObjectDirectoryRepository().findById(
                externalObjectDirectoryEntityOriginal.getId()).orElseThrow();
            assertThat(externalObjectDirectoryEntityAfter.getStatus().getId()).isEqualTo(
                ObjectRecordStatusEnum.ARM_RPO_PENDING.getId());
        });
    }
}
