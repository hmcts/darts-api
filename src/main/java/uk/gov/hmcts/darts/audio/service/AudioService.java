package uk.gov.hmcts.darts.audio.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.IOException;
import java.io.InputStream;

public interface AudioService {

    InputStream preview(Integer mediaId);

    void addAudio(MultipartFile audioFile, AddAudioMetadataRequest addAudioMetadata);

    void linkAudioAndHearing(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia);

    void emitterHeartBeat(SseEmitter emitter);

    @Async
    void pause10(SseEmitter emitter, Integer mediaId, String range) throws IOException;
}
