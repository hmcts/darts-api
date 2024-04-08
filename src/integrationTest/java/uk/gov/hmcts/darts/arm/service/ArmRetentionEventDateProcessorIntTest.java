package uk.gov.hmcts.darts.arm.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.component.ArmRetentionEventDateCalculator;
import uk.gov.hmcts.darts.arm.service.impl.ArmRetentionEventDateProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.MediaTestData;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;

@Slf4j
@Transactional
class ArmRetentionEventDateProcessorIntTest extends IntegrationBase {

    private static final LocalDate HEARING_DATE = LocalDate.of(2023, 6, 10);

    private static final OffsetDateTime MEDIA_RETENTION_DATE_TIME =
        OffsetDateTime.of(2023, 06, 10, 10, 50, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime RETENTION_DATE_TIME =
        OffsetDateTime.of(2023, 03, 02, 10, 50, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime START_TIME =
        OffsetDateTime.of(2023, 06, 10, 10, 00, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime END_TIME =
        OffsetDateTime.of(2023, 06, 10, 10, 45, 0, 0, ZoneOffset.UTC);

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Autowired
    private ArmRetentionEventDateCalculator armRetentionEventDateCalculator;

    @MockBean
    private ArmDataManagementApi armDataManagementApi;
    @MockBean
    private UserIdentity userIdentity;


    private ArmRetentionEventDateProcessor armRetentionEventDateProcessor;


    @BeforeEach
    void setupData() {
        armRetentionEventDateProcessor = new ArmRetentionEventDateProcessorImpl(externalObjectDirectoryRepository,
                                                                                armRetentionEventDateCalculator);
    }

    @Test
    void calculateEventDates() {

        // given
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                START_TIME,
                END_TIME,
                1
            ));
        savedMedia.setRetainUntilTs(MEDIA_RETENTION_DATE_TIME);
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            UUID.randomUUID()
        );

        armEod.setEventDateTs(END_TIME);
        armEod.setUpdateRetention(true);
        dartsDatabase.save(armEod);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        UpdateMetadataResponse updateMetadataResponse = UpdateMetadataResponse.builder().itemId(UUID.randomUUID())
            .cabinetId(101)
            .objectId(UUID.fromString("4bfe4fc7-4e2f-4086-8a0e-146cc4556260"))
            .objectType(1)
            .fileName("UpdateMetadata-20241801-122819.json")
            .isError(false)
            .responseStatus(0)
            .responseStatusMessages(null)
            .build();
        when(armDataManagementApi.updateMetadata(any(), any())).thenReturn(updateMetadataResponse);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", armEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertFalse(armEod.isUpdateRetention());
        assertEquals(0, armEod.getEventDateTs().truncatedTo(MILLIS).compareTo(RETENTION_DATE_TIME.truncatedTo(MILLIS)));

    }


    @Test
    void calculateEventDates_NoChanges() {

        // given
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                START_TIME,
                END_TIME,
                1
            ));
        savedMedia.setRetainUntilTs(MEDIA_RETENTION_DATE_TIME);
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            UUID.randomUUID()
        );

        armEod.setEventDateTs(RETENTION_DATE_TIME);
        armEod.setUpdateRetention(true);
        dartsDatabase.save(armEod);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        UpdateMetadataResponse updateMetadataResponse = UpdateMetadataResponse.builder().itemId(UUID.randomUUID())
            .cabinetId(101)
            .objectId(UUID.fromString("4bfe4fc7-4e2f-4086-8a0e-146cc4556260"))
            .objectType(1)
            .fileName("UpdateMetadata-20241801-122819.json")
            .isError(false)
            .responseStatus(0)
            .responseStatusMessages(null)
            .build();
        when(armDataManagementApi.updateMetadata(any(), any())).thenReturn(updateMetadataResponse);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", armEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertFalse(armEod.isUpdateRetention());
        assertEquals(0, armEod.getEventDateTs().truncatedTo(MILLIS).compareTo(RETENTION_DATE_TIME.truncatedTo(MILLIS)));

    }

    @Test
    void calculateEventDates_UpdateFails() {

        // given
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                START_TIME,
                END_TIME,
                1
            ));
        savedMedia.setRetainUntilTs(MEDIA_RETENTION_DATE_TIME);
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            UUID.randomUUID()
        );

        armEod.setEventDateTs(END_TIME);
        armEod.setUpdateRetention(true);
        dartsDatabase.save(armEod);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        UpdateMetadataResponse updateMetadataResponse = UpdateMetadataResponse.builder().itemId(UUID.randomUUID())
            .cabinetId(101)
            .objectId(UUID.fromString("4bfe4fc7-4e2f-4086-8a0e-146cc4556260"))
            .objectType(1)
            .fileName("UpdateMetadata-20241801-122819.json")
            .isError(false)
            .responseStatus(0)
            .responseStatusMessages(null)
            .build();
        when(armDataManagementApi.updateMetadata(any(), any())).thenReturn(updateMetadataResponse);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", armEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertFalse(armEod.isUpdateRetention());
        assertEquals(0, armEod.getEventDateTs().truncatedTo(MILLIS).compareTo(RETENTION_DATE_TIME.truncatedTo(MILLIS)));

    }
}
