package uk.gov.hmcts.darts.audio.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.PreviewRange;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.IOException;

public interface AudioService {

    ResponseEntity<byte[]> preview(Integer mediaId, PreviewRange previewRange) throws IOException;

    void addAudio(MultipartFile audioFile, AddAudioMetadataRequest addAudioMetadata);

    void linkAudioAndHearing(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia);
}
