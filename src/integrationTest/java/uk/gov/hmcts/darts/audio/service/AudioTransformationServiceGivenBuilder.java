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
import uk.gov.hmcts.darts.test.common.data.HearingTestData;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.builder.CustomExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.test.common.data.builder.CustomMediaEntity;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

import java.time.OffsetDateTime;
import java.util.List;

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

    private HearingEntity hearingEntityWithMedia1;
    private HearingEntity hearingEntityWithMedia2;
    private HearingEntity hearingEntityWithMedia4;
    private HearingEntity hearingEntityWithoutMedia;

    private MediaEntity mediaEntity1;
    private MediaEntity mediaEntity2;
    private MediaEntity mediaEntity3;
    private MediaEntity mediaEntity4;
    private static final OffsetDateTime MEDIA_START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_END_TIME = MEDIA_START_TIME.plusHours(1);

    public void setupTest() {
        hearingEntityWithMedia1 = HearingTestData.someMinimalHearing();
        hearingEntityWithMedia2 = HearingTestData.someMinimalHearing();
        hearingEntityWithMedia4 = HearingTestData.someMinimalHearing();
        hearingEntityWithoutMedia = HearingTestData.someMinimalHearing();
        CustomMediaEntity.CustomMediaBuilderRetrieve mediaTestData1 = getMediaTestData().someMinimal();
        CustomMediaEntity.CustomMediaBuilderRetrieve mediaTestData2 = getMediaTestData().someMinimal();
        CustomMediaEntity.CustomMediaBuilderRetrieve mediaTestData3 = getMediaTestData().someMinimal();
        CustomMediaEntity.CustomMediaBuilderRetrieve mediaTestData4 = getMediaTestData().someMinimal();

        int channel = 1;
        mediaTestData1.getBuilder().hearingList(List.of(hearingEntityWithMedia1)).channel(1);
        mediaTestData2.getBuilder().hearingList(List.of(hearingEntityWithMedia1, hearingEntityWithMedia2));
        mediaTestData4.getBuilder().hearingList(List.of(hearingEntityWithMedia1, hearingEntityWithMedia4)).isHidden(true).channel(channel);

        mediaEntity1 = mediaTestData1.build();
        mediaEntity2 = mediaTestData2.build();
        mediaEntity3 = mediaTestData3.build();
        mediaEntity4 = mediaTestData4.build();

        dartsDatabase.save(mediaEntity1);
        dartsDatabase.save(mediaEntity2);
        dartsDatabase.save(mediaEntity3);
        dartsDatabase.save(mediaEntity4);
    }

    public ExternalObjectDirectoryEntity externalObjectDirForMedia(MediaEntity mediaEntity) {
        ExternalObjectDirectoryTestData externalObjectDirectoryTestData = PersistableFactory.getExternalObjectDirectoryTestData();
        CustomExternalObjectDirectoryEntity.CustomExternalObjectDirectoryuilderRetrieve retrieve = externalObjectDirectoryTestData.someMinimal();

        retrieve.getBuilder().media(mediaEntity).status(dartsDatabase
                                                            .getObjectRecordStatusRepository()
                                                            .getReferenceById(STORED.getId()))
                                                            .externalLocationType(dartsDatabase
                                                            .getExternalLocationTypeRepository().getReferenceById(UNSTRUCTURED.getId()));
        return dartsDatabase.save(retrieve.build());
    }
}