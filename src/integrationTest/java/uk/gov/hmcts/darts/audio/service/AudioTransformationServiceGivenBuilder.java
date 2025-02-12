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
import uk.gov.hmcts.darts.test.common.data.builder.TestMediaEntity;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

import java.time.OffsetDateTime;

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
    private static final OffsetDateTime MEDIA_START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_END_TIME = MEDIA_START_TIME.plusHours(1);

    public void setupTest() {
        hearingEntityWithMedia1 = PersistableFactory.getHearingTestData().someMinimalHearing();
        hearingEntityWithMedia2 = PersistableFactory.getHearingTestData().someMinimalHearing();
        hearingEntityWithMedia3 = PersistableFactory.getHearingTestData().someMinimalHearing();
        hearingEntityWithMedia4 = PersistableFactory.getHearingTestData().someMinimalHearing();
        hearingEntityWithoutMedia = PersistableFactory.getHearingTestData().someMinimalHearing();
        TestMediaEntity.TestMediaBuilderRetrieve mediaTestData1 = getMediaTestData().someMinimalBuilderHolder();
        TestMediaEntity.TestMediaBuilderRetrieve mediaTestData2 = getMediaTestData().someMinimalBuilderHolder();
        TestMediaEntity.TestMediaBuilderRetrieve mediaTestData3 = getMediaTestData().someMinimalBuilderHolder();
        TestMediaEntity.TestMediaBuilderRetrieve mediaTestData4 = getMediaTestData().someMinimalBuilderHolder();

        int channel = 1;
        mediaTestData1.getBuilder().channel(1);
        mediaTestData4.getBuilder().isHidden(true).channel(channel);

        mediaEntity1 = mediaTestData1.build().getEntity();
        mediaEntity2 = mediaTestData2.build().getEntity();
        mediaEntity3 = mediaTestData3.build().getEntity();
        mediaEntity4 = mediaTestData4.build().getEntity();

        dartsPersistence.save(mediaEntity1);
        dartsPersistence.save(mediaEntity2);
        dartsPersistence.save(mediaEntity3);
        dartsPersistence.save(mediaEntity4);

        hearingEntityWithMedia1.addMedia(mediaEntity1);
        hearingEntityWithMedia1.addMedia(mediaEntity2);
        hearingEntityWithMedia3.addMedia(mediaEntity3);
        hearingEntityWithMedia4.addMedia(mediaEntity1);

        dartsPersistence.save(hearingEntityWithMedia1);
        dartsPersistence.save(hearingEntityWithMedia2);
        dartsPersistence.save(hearingEntityWithMedia4);
        dartsPersistence.save(hearingEntityWithoutMedia);
        dartsPersistence.save(mediaEntity3);
    }

    public ExternalObjectDirectoryEntity externalObjectDirForMedia(MediaEntity mediaEntity) {
        ExternalObjectDirectoryTestData externalObjectDirectoryTestData = PersistableFactory.getExternalObjectDirectoryTestData();
        TestExternalObjectDirectoryEntity.TestExternalObjectDirectoryuilderRetrieve retrieve = externalObjectDirectoryTestData.someMinimalBuilderHolder();

        retrieve.getBuilder().media(mediaEntity).status(dartsDatabase
                                                            .getObjectRecordStatusRepository()
                                                            .getReferenceById(STORED.getId()))
                                                            .externalLocationType(dartsDatabase
                                                            .getExternalLocationTypeRepository().getReferenceById(UNSTRUCTURED.getId()));
        return dartsDatabase.save(retrieve.build().getEntity());
    }
}