package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransientObjectDirectoryStub {
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final UserAccountStub userAccountStub;

    public TransientObjectDirectoryEntity createTransientObjectDirectoryEntity(MediaRequestEntity mediaRequestEntity,
                                                                               ObjectDirectoryStatusEntity objectDirectoryStatusEntity,
                                                                               UUID externalLocation) {
        var transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity.setMediaRequest(mediaRequestEntity);
        transientObjectDirectoryEntity.setLastModifiedBy(userAccountStub.getDefaultUser());
        transientObjectDirectoryEntity.setStatus(objectDirectoryStatusEntity);
        transientObjectDirectoryEntity.setExternalLocation(externalLocation);
        transientObjectDirectoryEntity.setLastModifiedDateTime(OffsetDateTime.now());
        transientObjectDirectoryRepository.save(transientObjectDirectoryEntity);
        return transientObjectDirectoryEntity;
    }

}
