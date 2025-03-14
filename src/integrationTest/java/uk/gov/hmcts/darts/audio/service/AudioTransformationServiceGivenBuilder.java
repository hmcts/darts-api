package uk.gov.hmcts.darts.audio.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.test.common.data.ExternalObjectDirectoryTestData;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.builder.TestExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaTestData;

@Transactional
@Service
@Getter
@RequiredArgsConstructor
@Scope(scopeName = SCOPE_PROTOTYPE)
public class AudioTransformationServiceGivenBuilder {

    private final DartsPersistence dartsDatabase;
    private final DartsPersistence dartsPersistence;

    private HearingEntity hearingEntityWithMedia1;
    private HearingEntity hearingEntityWithMedia2;
    private HearingEntity hearingEntityWithMedia3;
    private HearingEntity hearingEntityWithMedia4;
    private HearingEntity hearingEntityWithoutMedia;

    private MediaEntity mediaEntity1;
    private MediaEntity mediaEntity2;
    private MediaEntity mediaEntity3;
    private MediaEntity mediaEntity4;

    public void setupTest() {
        hearingEntityWithMedia1 = PersistableFactory.getHearingTestData().someMinimalHearing();
        hearingEntityWithMedia2 = PersistableFactory.getHearingTestData().someMinimalHearing();
        hearingEntityWithMedia3 = PersistableFactory.getHearingTestData().someMinimalHearing();
        hearingEntityWithMedia4 = PersistableFactory.getHearingTestData().someMinimalHearing();
        hearingEntityWithoutMedia = PersistableFactory.getHearingTestData().someMinimalHearing();
        mediaEntity1 = getMediaTestData().someMinimalBuilder()
            .isCurrent(true)
            .build()
            .getEntity();
        mediaEntity2 = getMediaTestData().someMinimalBuilder()
            .isCurrent(true)
            .build()
            .getEntity();
        mediaEntity3 = getMediaTestData().someMinimalBuilder()
            .isCurrent(true)
            .build()
            .getEntity();
        mediaEntity4 = getMediaTestData().someMinimalBuilder()
            .isCurrent(true)
            .isHidden(true)
            .build()
            .getEntity();

        hearingEntityWithMedia1.addMedia(mediaEntity1);
        hearingEntityWithMedia1.addMedia(mediaEntity2);
        hearingEntityWithMedia3.addMedia(mediaEntity3);
        hearingEntityWithMedia4.addMedia(mediaEntity1);

        dartsPersistence.save(hearingEntityWithMedia1);
        dartsPersistence.save(hearingEntityWithMedia2);
        dartsPersistence.save(hearingEntityWithMedia3);
        dartsPersistence.save(hearingEntityWithMedia4);
        dartsPersistence.save(hearingEntityWithoutMedia);
    }

    public ExternalObjectDirectoryEntity externalObjectDirForMedia(MediaEntity mediaEntity) {
        ExternalObjectDirectoryTestData externalObjectDirectoryTestData = PersistableFactory.getExternalObjectDirectoryTestData();
        TestExternalObjectDirectoryEntity.TestExternalObjectDirectoryBuilderRetrieve retrieve = externalObjectDirectoryTestData.someMinimalBuilderHolder();

        retrieve.getBuilder().media(mediaEntity).status(dartsDatabase
                                                            .getObjectRecordStatusRepository()
                                                            .getReferenceById(STORED.getId()))
            .externalLocationType(dartsDatabase
                                      .getExternalLocationTypeRepository().getReferenceById(UNSTRUCTURED.getId()));
        return dartsDatabase.save(retrieve.build().getEntity());
    }
}