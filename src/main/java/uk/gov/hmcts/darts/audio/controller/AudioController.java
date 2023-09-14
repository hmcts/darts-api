package uk.gov.hmcts.darts.audio.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audio.api.AudioApi;
import uk.gov.hmcts.darts.audio.component.AudioRequestSummaryMapper;
import uk.gov.hmcts.darts.audio.component.AudioResponseMapper;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.audio.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audio.model.AudioRequestSummary;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.InputStream;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AudioController implements AudioApi {

    private final MediaRequestService mediaRequestService;
    private final AudioTransformationService audioTransformationService;
    private final AudioResponseMapper audioResponseMapper;
    private final AudioRequestSummaryMapper audioRequestSummaryMapper;

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
    public ResponseEntity<Void> deleteAudioRequest(Integer audioRequestId) {
        mediaRequestService.deleteAudioRequest(audioRequestId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource> getProcessedAudio(Integer audioRequestId) {
        InputStream audioFileStream = mediaRequestService.getProcessedAudio(audioRequestId);

        return new ResponseEntity<>(new InputStreamResource(audioFileStream),
                                    HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<AudioMetadata>> getAudiosByHearing(Integer hearingId) {
        List<MediaEntity> mediaEntities = audioTransformationService.getMediaMetadata(hearingId);
        List<AudioMetadata> audioMetadata = audioResponseMapper.mapToAudioMetadata(mediaEntities);

        return new ResponseEntity<>(audioMetadata, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<AudioRequestSummary>> getUserAudioRequests(Integer userId, Boolean expired) {

        return new ResponseEntity<>(audioRequestSummaryMapper.mapToAudioRequestSummary(
            mediaRequestService.viewAudioRequests(userId, expired)), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<org.springframework.core.io.Resource> preview(Integer mediaId) {
        InputStream audioMediaFile = mediaRequestService.preview(mediaId);
        return new ResponseEntity<>(new InputStreamResource(audioMediaFile), HttpStatus.OK);
    }

}
