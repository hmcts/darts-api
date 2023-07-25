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
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.testutils.data.ExternalObjectDirectoryTestData;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.util.UUID;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEnum.STORED;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createCaseAtCourthouse;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.createCourthouse;
import static uk.gov.hmcts.darts.testutils.data.CourtroomTestData.createCourtRoomAtCourthouse;
import static uk.gov.hmcts.darts.testutils.data.HearingMediaTestData.createMedia;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createHearingWith;

@Transactional
@Service
@Getter
@RequiredArgsConstructor
@Scope(scopeName = SCOPE_PROTOTYPE)
public class AudioTransformationServiceGivenBuilder {

    private final DartsDatabaseStub dartsDatabase;

    private HearingEntity hearingEntityWithMedia1;
    private HearingEntity hearingEntityWithMedia2;
    private HearingEntity hearingEntityWithoutMedia;
    private MediaEntity mediaEntity1;
    private MediaEntity mediaEntity2;
    private MediaEntity mediaEntity3;
    private CourtroomEntity courtroomAtNewcastle;
    private CourtCaseEntity courtCase;

    public void setupTest() {
        courtroomAtNewcastle = getCourtroomAtNewcastle();
        courtCase = dartsDatabase.save(createCaseAtCourthouse("c1", courtroomAtNewcastle.getCourthouse()));
        hearingEntityWithMedia1 = dartsDatabase.save(createHearingWith(courtCase, courtroomAtNewcastle));
        hearingEntityWithMedia2 = dartsDatabase.save(createHearingWith(courtCase, courtroomAtNewcastle));
        hearingEntityWithoutMedia = dartsDatabase.save(createHearingWith(courtCase, courtroomAtNewcastle));

        mediaEntity1 = dartsDatabase.addMediaToHearing(hearingEntityWithMedia1, createMedia(courtroomAtNewcastle)).getMedia();
        mediaEntity2 = dartsDatabase.addMediaToHearing(hearingEntityWithMedia1, createMedia(courtroomAtNewcastle)).getMedia();

        mediaEntity3 = createMedia(courtroomAtNewcastle);
    }

    public ExternalObjectDirectoryEntity externalObjectDirForMedia(MediaEntity mediaEntity) {
        var externalObjectDirectoryEntity1 = ExternalObjectDirectoryTestData.createExternalObjectDirectory(
              mediaEntity,
              dartsDatabase.getObjectDirectoryStatusRepository().getReferenceById(STORED.getId()),
              dartsDatabase.getExternalLocationTypeRepository().getReferenceById(UNSTRUCTURED.getId()),
              UUID.randomUUID()
        );
        return dartsDatabase.save(externalObjectDirectoryEntity1);
    }

    private CourtroomEntity getCourtroomAtNewcastle() {
        return dartsDatabase.save(
              createCourtRoomAtCourthouse(
                    createCourthouse("NEWCASTLE")));

    }
}
