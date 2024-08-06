package uk.gov.hmcts.darts.audio.service;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.test.common.data.JudgeTestData.createJudgeWithName;
import static uk.gov.hmcts.darts.test.common.data.MediaTestData.createMediaFor;
import static uk.gov.hmcts.darts.test.common.data.MediaTestData.createMediaWith;

@Transactional
@Service
@Getter
@RequiredArgsConstructor
@Scope(scopeName = SCOPE_PROTOTYPE)
public class AudioTransformationServiceGivenBuilder {

    private final DartsDatabaseStub dartsDatabase;
    private final ExternalObjectDirectoryStub externalObjectDirectoryStub;

    private HearingEntity hearingEntityWithMedia1;
    private HearingEntity hearingEntityWithMedia2;
    private MediaEntity mediaEntity1Hearing2;
    private HearingEntity hearingEntityWithoutMedia;
    private MediaEntity mediaEntity1;
    private MediaEntity mediaEntity2;
    private MediaEntity mediaEntity3;
    private MediaEntity mediaEntity4;
    private CourtroomEntity courtroomAtNewcastle;
    private JudgeEntity judge;

    private static final OffsetDateTime MEDIA_START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_END_TIME = MEDIA_START_TIME.plusHours(1);

    public void setupTest() {
        courtroomAtNewcastle = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        hearingEntityWithMedia1 = dartsDatabase.createHearing(
            "NEWCASTLE",
            "room_a",
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );
        judge = dartsDatabase.save(createJudgeWithName("aJudge"));
        hearingEntityWithMedia1.addJudge(judge, false);
        hearingEntityWithMedia2 = dartsDatabase.createHearing(
            "NEWCASTLE",
            "room_a",
            "c1",
            LocalDateTime.of(2020, 6, 21, 10, 0, 0)
        );
        hearingEntityWithMedia2.addJudge(judge, false);
        hearingEntityWithoutMedia = dartsDatabase.createHearing(
            "NEWCASTLE",
            "room_a",
            "c1",
            LocalDateTime.of(2020, 6, 22, 10, 0, 0)
        );
        hearingEntityWithoutMedia.addJudge(judge, false);

        int channel = 1;

        mediaEntity1 = dartsDatabase.addMediaToHearingNonTransactional(hearingEntityWithMedia1, createMediaWith(
            courtroomAtNewcastle, MEDIA_START_TIME, MEDIA_END_TIME, channel));

        mediaEntity2 = dartsDatabase.addMediaToHearingNonTransactional(hearingEntityWithMedia1, createMediaFor(courtroomAtNewcastle));

        mediaEntity1Hearing2 = dartsDatabase.addMediaToHearingNonTransactional(hearingEntityWithMedia2, createMediaWith(
            courtroomAtNewcastle, MEDIA_START_TIME, MEDIA_END_TIME, channel));

        mediaEntity3 = createMediaFor(courtroomAtNewcastle);

        mediaEntity4 = dartsDatabase.addMediaToHearingNonTransactional(hearingEntityWithMedia1, createMediaWith(
            courtroomAtNewcastle, MEDIA_START_TIME, MEDIA_END_TIME, channel));
        mediaEntity4.setHidden(true);
    }

    public ExternalObjectDirectoryEntity externalObjectDirForMedia(MediaEntity mediaEntity) {
        var externalObjectDirectoryEntity1 = externalObjectDirectoryStub.createExternalObjectDirectory(
            mediaEntity,
            dartsDatabase.getObjectRecordStatusRepository().getReferenceById(STORED.getId()),
            dartsDatabase.getExternalLocationTypeRepository().getReferenceById(UNSTRUCTURED.getId()),
            UUID.randomUUID()
        );
        return dartsDatabase.save(externalObjectDirectoryEntity1);
    }
}