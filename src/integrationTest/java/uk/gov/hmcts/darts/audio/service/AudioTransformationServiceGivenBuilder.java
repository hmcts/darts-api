package uk.gov.hmcts.darts.audio.service;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;
import static uk.gov.hmcts.darts.testutils.data.JudgeTestData.createJudgeWithName;
import static uk.gov.hmcts.darts.testutils.data.MediaTestData.createMediaFor;

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
    private HearingEntity hearingEntityWithoutMedia;
    private MediaEntity mediaEntity1;
    private MediaEntity mediaEntity2;
    private MediaEntity mediaEntity3;
    private CourtroomEntity courtroomAtNewcastle;
    private CourtCaseEntity courtCase;
    private JudgeEntity judge;

    public void setupTest() {
        hearingEntityWithMedia1 = dartsDatabase.createHearing(
            "NEWCASTLE",
            "room_a",
            "c1",
            LocalDate.of(2020, 6, 20)
        );
        judge = dartsDatabase.save(createJudgeWithName("aJudge"));
        hearingEntityWithMedia1.addJudge(judge);
        hearingEntityWithMedia2 = dartsDatabase.createHearing(
            "NEWCASTLE",
            "room_a",
            "c1",
            LocalDate.of(2020, 6, 21)
        );
        hearingEntityWithMedia2.addJudge(judge);
        hearingEntityWithoutMedia = dartsDatabase.createHearing(
            "NEWCASTLE",
            "room_a",
            "c1",
            LocalDate.of(2020, 6, 22)
        );
        hearingEntityWithoutMedia.addJudge(judge);

        mediaEntity1 = dartsDatabase.addMediaToHearing(hearingEntityWithMedia1, createMediaFor(courtroomAtNewcastle));
        mediaEntity2 = dartsDatabase.addMediaToHearing(hearingEntityWithMedia1, createMediaFor(courtroomAtNewcastle));

        mediaEntity3 = createMediaFor(courtroomAtNewcastle);
    }

    public ExternalObjectDirectoryEntity externalObjectDirForMedia(MediaEntity mediaEntity) {
        var externalObjectDirectoryEntity1 = externalObjectDirectoryStub.createExternalObjectDirectory(
            mediaEntity,
            dartsDatabase.getObjectDirectoryStatusRepository().getReferenceById(STORED.getId()),
            dartsDatabase.getExternalLocationTypeRepository().getReferenceById(UNSTRUCTURED.getId()),
            UUID.randomUUID()
        );
        return dartsDatabase.save(externalObjectDirectoryEntity1);
    }
}
