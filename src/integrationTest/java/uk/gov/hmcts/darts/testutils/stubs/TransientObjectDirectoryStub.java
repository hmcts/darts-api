package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransientObjectDirectoryStub {

    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final UserAccountStub userAccountStub;
    private final TransformedMediaStub transformedMediaStub;

    public TransientObjectDirectoryEntity createTransientObjectDirectoryEntity(MediaRequestEntity mediaRequestEntity,
                                                                               ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                               UUID externalLocation) {
        TransformedMediaEntity transformedMediaEntity = transformedMediaStub.createTransformedMediaEntity(mediaRequestEntity, null, null, null);
        return createTransientObjectDirectoryEntity(transformedMediaEntity, objectRecordStatusEntity, externalLocation);
    }


    public TransientObjectDirectoryEntity createTransientObjectDirectoryEntity(MediaRequestEntity mediaRequestEntity,
                                                                               ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                               UUID externalLocation, OffsetDateTime lastAccessedDate) {
        TransformedMediaEntity transformedMediaEntity = transformedMediaStub.createTransformedMediaEntity(mediaRequestEntity, null, null, lastAccessedDate);
        return createTransientObjectDirectoryEntity(transformedMediaEntity, objectRecordStatusEntity, externalLocation);
    }

    public TransientObjectDirectoryEntity createTransientObjectDirectoryEntity(TransformedMediaEntity transformedMediaEntity,
                                                                               ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                               UUID externalLocation) {
        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setTransformedMedia(transformedMediaEntity);
        transientObjectDirectoryEntity.setLastModifiedBy(userAccountStub.getIntegrationTestUserAccountEntity());
        transientObjectDirectoryEntity.setStatus(objectRecordStatusEntity);
        transientObjectDirectoryEntity.setExternalLocation(externalLocation);
        transientObjectDirectoryEntity.setLastModifiedDateTime(OffsetDateTime.now());
        transientObjectDirectoryRepository.saveAndFlush(transientObjectDirectoryEntity);
        return transientObjectDirectoryEntity;
    }

}
