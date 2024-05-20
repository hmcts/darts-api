package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.audio.model.SearchTransformedMediaRequest;
import uk.gov.hmcts.darts.audio.model.SearchTransformedMediaResponse;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.util.List;


public interface AudioService {

    List<MediaEntity> getAudioMetadata(Integer hearingId, Integer channel);

    BinaryData encode(Integer mediaId);

    void addAudio(MultipartFile audioFile, AddAudioMetadataRequest addAudioMetadata);

    void linkAudioToHearingInMetadata(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity mediaEntityToReplace, MediaEntity savedMedia);

    void linkAudioToHearingByEvent(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia);

    void setIsArchived(List<AudioMetadata> audioMetadata, Integer hearingId);

    void setIsAvailable(List<AudioMetadata> audioMetadata);

    List<SearchTransformedMediaResponse> searchForTransformedMedia(SearchTransformedMediaRequest getTransformedMediaRequest);
}