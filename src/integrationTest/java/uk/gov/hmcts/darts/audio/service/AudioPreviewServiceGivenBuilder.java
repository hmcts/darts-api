package uk.gov.hmcts.darts.audio.service;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Transactional
@Service
@Getter
@RequiredArgsConstructor
@Scope(scopeName = SCOPE_PROTOTYPE)
public class AudioPreviewServiceGivenBuilder {

    private final DartsDatabaseStub dartsDatabase;
    private final ExternalObjectDirectoryStub externalObjectDirectoryStub;

    private static final OffsetDateTime MEDIA_START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime MEDIA_END_TIME = MEDIA_START_TIME.plusHours(1);
    private final DartsPersistence dartsPersistence;

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