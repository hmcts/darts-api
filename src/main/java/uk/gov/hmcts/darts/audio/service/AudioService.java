package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.util.List;


public interface AudioService {

    List<MediaEntity> getMediaEntitiesByHearingAndChannel(Integer hearingId, Integer channel);

    BinaryData encode(Long mediaId);

    void setIsArchived(List<AudioMetadata> audioMetadata, Integer hearingId);

    void setIsAvailable(List<AudioMetadata> audioMetadata);

}