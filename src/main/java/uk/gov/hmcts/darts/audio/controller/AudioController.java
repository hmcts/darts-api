package uk.gov.hmcts.darts.audio.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audio.api.AudioApi;
import uk.gov.hmcts.darts.audio.component.AudioResponseMapper;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.audio.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.InputStream;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AudioController implements AudioApi {

    private final MediaRequestService mediaRequestService;
    private final AudioService audioService;
    private final AudioTransformationService audioTransformationService;
    private final AudioResponseMapper audioResponseMapper;

    @Override
    public ResponseEntity<Void> addAudioRequest(AudioRequestDetails audioRequestDetails) {
        try {
            mediaRequestService.saveAudioRequest(audioRequestDetails);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Resource> download(Integer audioRequestId) {
        InputStream audioFileStream = audioService.download(audioRequestId);

        return new ResponseEntity<>(new InputStreamResource(audioFileStream),
                                    HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<AudioMetadata>> getAudioMetadata(Integer hearingId) {
        List<MediaEntity> mediaEntities = audioTransformationService.getMediaMetadata(hearingId);
        List<AudioMetadata> audioMetadata = audioResponseMapper.mapToAudioMetadata(mediaEntities);

        return new ResponseEntity<>(audioMetadata, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource> preview(Integer mediaId) {
        InputStream audioMediaFile = audioService.preview(mediaId);
        return new ResponseEntity<>(new InputStreamResource(audioMediaFile), HttpStatus.OK);
    }

}
