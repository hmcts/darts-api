package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class TransformedMediaStub {

    private final TransformedMediaRepository transformedMediaRepository;
    private final UserAccountStub userAccountStub;

    public TransformedMediaEntity createTransformedMediaEntity(MediaRequestEntity mediaRequestEntity) {
        return createTransformedMediaEntity(mediaRequestEntity, null, null, null);
    }

    public TransformedMediaEntity createTransformedMediaEntity(MediaRequestEntity mediaRequestEntity, String filename, OffsetDateTime expiry,
          OffsetDateTime lastAccessed) {
        TransformedMediaEntity transformedMediaEntity = new TransformedMediaEntity();
        transformedMediaEntity.setMediaRequest(mediaRequestEntity);
        transformedMediaEntity.setStartTime(mediaRequestEntity.getStartTime());
        transformedMediaEntity.setEndTime(mediaRequestEntity.getEndTime());
        transformedMediaEntity.setLastModifiedBy(userAccountStub.getIntegrationTestUserAccountEntity());
        transformedMediaEntity.setCreatedBy(userAccountStub.getIntegrationTestUserAccountEntity());
        transformedMediaEntity.setCreatedDateTime(mediaRequestEntity.getCreatedDateTime());
        transformedMediaEntity.setOutputFilename(filename);
        if (filename != null) {
            transformedMediaEntity.setOutputFormat(AudioRequestOutputFormat.ZIP);
        }
        transformedMediaEntity.setExpiryTime(expiry);
        transformedMediaEntity.setLastAccessed(lastAccessed);
        return transformedMediaRepository.save(transformedMediaEntity);
    }

}
