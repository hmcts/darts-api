package uk.gov.hmcts.darts.audio.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audio.api.AudioRequestsApi;
import uk.gov.hmcts.darts.audio.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;

@RestController
@RequiredArgsConstructor
public class AudioRequestController implements AudioRequestsApi {

    private final MediaRequestService mediaRequestService;

    @Override
    public ResponseEntity<Void> addAudioRequest(AudioRequestDetails audioRequestDetails) {
        try {
            mediaRequestService.saveAudioRequest(audioRequestDetails);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

}
