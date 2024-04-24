package uk.gov.hmcts.darts.audio.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.MediaTestData;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.MARKED_FOR_DELETION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

class UnstructuredAudioDeleterProcessorTest extends IntegrationBase {

    public static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 6, 10, 10, 0, 0);

    @Autowired
    private UnstructuredAudioDeleterProcessor unstructuredAudioDeleterProcessor;

    @MockBean
    private CurrentTimeHelper currentTimeHelper;

    @Test
    void storedInArmAndLastUpdatedInUnstructuredMoreThan30WeeksAgo() {
        when(currentTimeHelper.currentOffsetDateTime())
            .thenReturn(OffsetDateTime.now().plusWeeks(35));
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

        ExternalObjectDirectoryEntity unstructuredEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            EodHelper.storedStatus(),
            EodHelper.unstructuredLocation(),
            uuid
        );
        dartsDatabase.save(unstructuredEod);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            EodHelper.storedStatus(),
            EodHelper.armLocation(),
            uuid
        );
        dartsDatabase.save(armEod);

        unstructuredAudioDeleterProcessor.markForDeletion();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository().findByMediaAndExternalLocationType(
            savedMedia,
            EodHelper.unstructuredLocation()
        );

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(MARKED_FOR_DELETION.getId(), foundMedia.getStatus().getId());
    }

    @Test
    void storedInArmAndLastUpdatedInUnstructuredLessThan30WeeksAgo() {
        when(currentTimeHelper.currentOffsetDateTime())
            .thenReturn(OffsetDateTime.now().plusWeeks(25));
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

        ExternalObjectDirectoryEntity unstructuredEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            EodHelper.storedStatus(),
            EodHelper.unstructuredLocation(),
            uuid
        );
        dartsDatabase.save(unstructuredEod);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            EodHelper.storedStatus(),
            EodHelper.armLocation(),
            uuid
        );
        dartsDatabase.save(armEod);

        unstructuredAudioDeleterProcessor.markForDeletion();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository().findByMediaAndExternalLocationType(
            savedMedia,
            EodHelper.unstructuredLocation()
        );

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(STORED.getId(), foundMedia.getStatus().getId());
    }
}
