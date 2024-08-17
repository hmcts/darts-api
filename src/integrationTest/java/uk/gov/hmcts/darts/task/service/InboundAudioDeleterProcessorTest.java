package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.audio.service.InboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.test.common.data.MediaTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.MARKED_FOR_DELETION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Disabled("Impacted by V1_367__adding_not_null_constraints_part_4.sql")
@SuppressWarnings("PMD.ExcessiveImports")
class InboundAudioDeleterProcessorTest extends IntegrationBase {

    public static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 6, 10, 10, 0, 0);

    @Autowired
    private InboundAudioDeleterProcessor inboundAudioDeleterProcessor;

    @MockBean
    private CurrentTimeHelper currentTimeHelper;

    @Test
    void updatedMoreThan24HrsAgo() {
        when(currentTimeHelper.currentOffsetDateTime())
            .thenReturn(OffsetDateTime.now().plusHours(500));
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));

        UUID uuid = UUID.fromString("075987ea-b34d-49c7-b8db-439bfbe2496c");

        ExternalObjectDirectoryEntity inboundEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.INBOUND),
            uuid
        );
        dartsDatabase.save(inboundEod);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            uuid
        );
        dartsDatabase.save(armEod);

        inboundAudioDeleterProcessor.markForDeletion();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository().findByMediaAndExternalLocationType(
            savedMedia,
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.INBOUND)
        );

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(MARKED_FOR_DELETION.getId(), foundMedia.getStatus().getId());
    }


    @Test
    void updatedLessThan24HrsAgo() {
        when(currentTimeHelper.currentOffsetDateTime())
            .thenReturn(OffsetDateTime.now().plusHours(1));
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));

        UUID uuid = UUID.fromString("075987ea-b34d-49c7-b8db-439bfbe2496c");

        ExternalObjectDirectoryEntity inboundEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.INBOUND),
            uuid
        );
        dartsDatabase.save(inboundEod);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            uuid
        );
        dartsDatabase.save(armEod);

        inboundAudioDeleterProcessor.markForDeletion();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository().findByMediaAndExternalLocationType(
            savedMedia,
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.INBOUND)
        );


        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(STORED.getId(), foundMedia.getStatus().getId());
    }
}
