package uk.gov.hmcts.darts.audio.service;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.InputStream;
import java.util.List;


public interface AudioService {

    List<MediaEntity> getAudioMetadata(Integer hearingId, Integer channel);

    InputStream preview(Integer mediaId);

    void addAudio(MultipartFile audioFile, AddAudioMetadataRequest addAudioMetadata);

    void linkAudioToHearingInMetadata(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia);

    void linkAudioToHearingByEvent(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia);

    SseEmitter startStreamingPreview(Integer mediaId, String range, SseEmitter emitter);

}
