package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Deprecated
public class TransientObjectDirectoryStub {

    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final UserAccountStub userAccountStub;
    private final TransformedMediaStub transformedMediaStub;

    public TransientObjectDirectoryEntity createTransientObjectDirectoryEntity(MediaRequestEntity mediaRequestEntity,
                                                                               ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                               String externalLocation) {
        TransformedMediaEntity transformedMediaEntity = transformedMediaStub.createTransformedMediaEntity(mediaRequestEntity, null, null, null);
        return createTransientObjectDirectoryEntity(transformedMediaEntity, objectRecordStatusEntity, externalLocation);
    }


    public TransientObjectDirectoryEntity createTransientObjectDirectoryEntity(MediaRequestEntity mediaRequestEntity,
                                                                               ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                               String externalLocation, OffsetDateTime lastAccessedDate) {
        TransformedMediaEntity transformedMediaEntity = transformedMediaStub.createTransformedMediaEntity(mediaRequestEntity, null, null, lastAccessedDate);
        return createTransientObjectDirectoryEntity(transformedMediaEntity, objectRecordStatusEntity, externalLocation);
    }

    public TransientObjectDirectoryEntity createTransientObjectDirectoryEntity(TransformedMediaEntity transformedMediaEntity,
                                                                               ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                               String externalLocation) {
        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setTransformedMedia(transformedMediaEntity);
        var userAccount = userAccountStub.getIntegrationTestUserAccountEntity();
        transientObjectDirectoryEntity.setLastModifiedBy(userAccount);
        transientObjectDirectoryEntity.setCreatedBy(userAccount);
        transientObjectDirectoryEntity.setStatus(objectRecordStatusEntity);
        transientObjectDirectoryEntity.setExternalLocation(externalLocation);
        transientObjectDirectoryEntity.setLastModifiedDateTime(OffsetDateTime.parse("2024-02-12T13:45:00Z"));
        return transientObjectDirectoryRepository.saveAndFlush(transientObjectDirectoryEntity);
    }

}