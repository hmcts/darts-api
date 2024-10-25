package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.DETS;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaTestData;

class DetsToArmBatchPushProcessorIntTest extends IntegrationBase {

    private static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 9, 26, 10, 0, 0);
    private MediaEntity savedMedia;

    @MockBean
    private UserIdentity userIdentity;
    @MockBean
    private ArmDataManagementApi armDataManagementApi;

    @Autowired
    private DetsToArmBatchPushProcessor detsToArmBatchPushProcessor;


    @BeforeEach
    void setupData() {

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        HearingEntity hearing = dartsDatabase.createHearing(
            "Bristol",
            "Int Test Courtroom 1",
            "1",
            HEARING_DATE
        );

        savedMedia = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));
        savedMedia = dartsDatabase.save(savedMedia);
    }

    @Test
    void processDetsToArmWithDetsEodSuccess() {
        // given
        ObjectStateRecordEntity objectStateRecordEntity = dartsDatabase.getObjectStateRecordRepository()
            .save(createObjectStateRecordEntity(111l));
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        ExternalObjectDirectoryEntity detsEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            DETS,
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        detsEod.setLastModifiedDateTime(latestDateTime);
        detsEod.setTransferAttempts(1);
        detsEod.setResponseCleaned(false);
        detsEod.setOsrUuid(objectStateRecordEntity.getUuid());
        detsEod = dartsDatabase.save(detsEod);

        objectStateRecordEntity.setEodId(String.valueOf(detsEod.getId()));
        String rawFilename = String.format("%s_%s_%s", detsEod.getId(), savedMedia.getId(), detsEod.getTransferAttempts());
        doNothing().when(armDataManagementApi).copyDetsBlobDataToArm(detsEod.getExternalLocation().toString(), rawFilename);

        // when
        detsToArmBatchPushProcessor.processDetsToArm(5);

        // then
        Optional<ExternalObjectDirectoryEntity> foundMediaEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findMatchingExternalObjectDirectoryEntityByLocation(
                EodHelper.storedStatus(),
                EodHelper.detsLocation(),
                detsEod.getMedia(),
                detsEod.getTranscriptionDocumentEntity(),
                detsEod.getAnnotationDocumentEntity(),
                detsEod.getCaseDocument()
            );
        assertTrue(foundMediaEod.isPresent());
        ExternalObjectDirectoryEntity foundMedia = foundMediaEod.get();
        assertEquals(EodHelper.armDropZoneStatus(), foundMedia.getStatus());

    }

    @Test
    void processDetsToArmWithNoOsrUuid() {
        // given
        ExternalObjectDirectoryEntity detsEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            DETS,
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        detsEod.setLastModifiedDateTime(latestDateTime);
        detsEod.setTransferAttempts(1);
        detsEod.setResponseCleaned(false);
        dartsDatabase.save(detsEod);

        // when
        assertThrows(DartsException.class, () -> detsToArmBatchPushProcessor.processDetsToArm(5));
    }

    private ObjectStateRecordEntity createObjectStateRecordEntity(Long uuid) {
        ObjectStateRecordEntity objectStateRecordEntity = new ObjectStateRecordEntity();
        objectStateRecordEntity.setUuid(uuid);
        return objectStateRecordEntity;
    }
}
