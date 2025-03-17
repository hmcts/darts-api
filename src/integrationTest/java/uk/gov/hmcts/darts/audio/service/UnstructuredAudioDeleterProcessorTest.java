package uk.gov.hmcts.darts.audio.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.DatabaseDateSetter;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.MARKED_FOR_DELETION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

class UnstructuredAudioDeleterProcessorTest extends IntegrationBase {

    private static final String USER_EMAIL_ADDRESS = "system_UnstructuredAudioDeleter@hmcts.net";
    public static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 6, 10, 10, 0, 0);

    @Autowired
    private UnstructuredAudioDeleterProcessor unstructuredAudioDeleterProcessor;

    @Autowired
    private DatabaseDateSetter dateConfigurer;

    @Test
    void storedInArmAndLastUpdatedInUnstructuredMoreThan30WeeksAgo() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        anAuthenticatedUserFor(USER_EMAIL_ADDRESS);
        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsPersistence.save(hearing);

        MediaEntity savedMedia = dartsPersistence.save(
            PersistableFactory.getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));

        String uuid = "075987ea-b34d-49c7-b8db-439bfbe2496c";

        ExternalObjectDirectoryEntity unstructuredEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            EodHelper.storedStatus(),
            EodHelper.unstructuredLocation(),
            uuid
        );
        dartsDatabase.getExternalObjectDirectoryRepository().saveAndFlush(unstructuredEod);
        dateConfigurer.setLastModifiedDateNoRefresh(unstructuredEod, OffsetDateTime.now().minusWeeks(35));

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            EodHelper.storedStatus(),
            EodHelper.armLocation(),
            uuid
        );
        dartsDatabase.save(armEod);

        unstructuredAudioDeleterProcessor.markForDeletion(1000);

        transactionalUtil.executeInTransaction(() -> {
            List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase
                .getExternalObjectDirectoryRepository().findByMediaAndExternalLocationType(
                    savedMedia,
                    EodHelper.unstructuredLocation()
                );

            assertEquals(1, foundMediaList.size());
            ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
            assertEquals(MARKED_FOR_DELETION.getId(), foundMedia.getStatus().getId());
            assertEquals(USER_EMAIL_ADDRESS, foundMedia.getLastModifiedBy().getEmailAddress());
        });
    }

    @Test
    void storedInArmAndLastUpdatedInUnstructuredLessThan30WeeksAgo() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsPersistence.save(hearing);

        MediaEntity savedMedia = dartsPersistence.save(
            PersistableFactory.getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));

        String uuid = "075987ea-b34d-49c7-b8db-439bfbe2496c";

        ExternalObjectDirectoryEntity unstructuredEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            EodHelper.storedStatus(),
            EodHelper.unstructuredLocation(),
            uuid
        );
        dartsDatabase.getExternalObjectDirectoryRepository().saveAndFlush(unstructuredEod);
        dateConfigurer.setLastModifiedDateNoRefresh(unstructuredEod, OffsetDateTime.now().minusWeeks(25));

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            EodHelper.storedStatus(),
            EodHelper.armLocation(),
            uuid
        );
        dartsDatabase.save(armEod);

        unstructuredAudioDeleterProcessor.markForDeletion(1000);

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository().findByMediaAndExternalLocationType(
            savedMedia,
            EodHelper.unstructuredLocation()
        );

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
        assertEquals(STORED.getId(), foundMedia.getStatus().getId());
    }
}