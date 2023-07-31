package uk.gov.hmcts.darts.audio.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audio.api.AudioApi;
import uk.gov.hmcts.darts.audio.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;

import java.io.InputStream;

@RestController
@RequiredArgsConstructor
public class AudioController implements AudioApi {

    private final MediaRequestService mediaRequestService;
    private final AudioService audioService;

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

}
