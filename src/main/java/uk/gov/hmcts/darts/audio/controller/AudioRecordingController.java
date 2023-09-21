package uk.gov.hmcts.darts.audio.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audiorecording.api.AudioRecordingApi;
import uk.gov.hmcts.darts.audiorecording.model.AddAudioRequest;

@RestController
@RequiredArgsConstructor
public class AudioRecordingController implements AudioRecordingApi {

    private final AudioService audioService;

    @Override
    public ResponseEntity<Void> addAudio(@Parameter(name = "RecordingPostRequestBody")
                                         @Valid @RequestBody(required = false) AddAudioRequest addAudioRequest) {

        audioService.addAudio(addAudioRequest);
        return new ResponseEntity<>(HttpStatus.OK);

    }
}
