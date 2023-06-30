package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;

import java.util.List;
import java.util.UUID;

public interface AudioTransformationService {

    MediaRequestEntity processAudioRequest(Integer requestId);

    BinaryData getAudioBlobData(UUID location);

    UUID saveAudioBlobData(BinaryData binaryData);

    TransientObjectDirectoryEntity saveTransientDataLocation(MediaRequestEntity mediaRequest, UUID externalLocation);

    List<MediaEntity> getMediaMetadata(Integer hearingId);

}
