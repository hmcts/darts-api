package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;

import java.util.UUID;

public interface AudioTransformationService {

    MediaRequestEntity processAudioRequest(Integer requestId);

    BinaryData getAudioBlobData(UUID location);

}
