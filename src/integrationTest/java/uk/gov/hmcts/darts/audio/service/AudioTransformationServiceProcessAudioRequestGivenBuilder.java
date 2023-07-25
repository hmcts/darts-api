package uk.gov.hmcts.darts.audio.service;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingMediaEntity;
import uk.gov.hmcts.darts.testutils.data.ExternalObjectDirectoryTestData;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEnum.STORED;

@Transactional
@Service
@Getter
@RequiredArgsConstructor
@Setter
@Scope(scopeName = SCOPE_PROTOTYPE)
@SuppressWarnings("MethodName")
public class AudioTransformationServiceProcessAudioRequestGivenBuilder {

    public static final int SOME_REQUESTOR = 666;
    public static final OffsetDateTime TIME_12_00 = OffsetDateTime.parse("2023-01-01T12:00Z");
    public static final OffsetDateTime TIME_12_10 = OffsetDateTime.parse("2023-01-01T12:10Z");
    public static final OffsetDateTime TIME_13_00 = OffsetDateTime.parse("2023-01-01T13:00Z");

    private final DartsDatabaseStub dartsDatabase;

    private MediaRequestEntity mediaRequestEntity;
    private HearingEntity hearingEntity;

    public void databaseIsProvisionedForHappyPath() {
        var mediaEntity = dartsDatabase.createMediaEntity(
              TIME_12_00,
              TIME_12_10,
              1
        );

        var hearingMediaEntity = new HearingMediaEntity();
        hearingMediaEntity.setMedia(mediaEntity);
        hearingMediaEntity.setHearing(dartsDatabase.getHearingRepository().getReferenceById(hearingEntity.getId()));

        dartsDatabase.getHearingMediaRepository()
              .saveAndFlush(hearingMediaEntity);

        var externalLocationTypeEntity = dartsDatabase.getExternalLocationTypeEntity(
              ExternalLocationTypeEnum.UNSTRUCTURED);
        var objectDirectoryStatusEntity = dartsDatabase.getObjectDirectoryStatusEntity(STORED);

        var externalObjectDirectoryEntity = ExternalObjectDirectoryTestData.createExternalObjectDirectory(
              mediaEntity,
              objectDirectoryStatusEntity,
              externalLocationTypeEntity,
              UUID.randomUUID()
        );
        dartsDatabase.getExternalObjectDirectoryRepository()
              .saveAndFlush(externalObjectDirectoryEntity);
    }

    public HearingEntity aHearingWith(String caseNumber, String courthouseName, String courtroomName) {
        hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
              caseNumber,
              courthouseName,
              courtroomName,
              LocalDate.now()
        );

        return hearingEntity;
    }

    public void aMediaRequestEntityFor(HearingEntity hearing) {
        mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearing(hearing);
        mediaRequestEntity.setRequestor(SOME_REQUESTOR);
        mediaRequestEntity.setStartTime(TIME_12_00);
        mediaRequestEntity.setEndTime(TIME_13_00);

        dartsDatabase.getMediaRequestRepository().saveAndFlush(mediaRequestEntity);
    }
}
