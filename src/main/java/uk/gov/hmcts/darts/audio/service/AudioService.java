package uk.gov.hmcts.darts.audio.service;

import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.InputStream;
import java.util.List;

public interface AudioService {

    List<MediaEntity> getAudioMetadata(Integer hearingId, Integer channel);

    InputStream preview(Integer mediaId);

    Flux<ServerSentEvent<ResponseEntity<byte[]>>> getAudioPreviewFlux(Integer mediaId, String range);

    void addAudio(MultipartFile audioFile, AddAudioMetadataRequest addAudioMetadata);

    void linkAudioToHearingInMetadata(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia);

    void linkAudioToHearingByEvent(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia);
}
