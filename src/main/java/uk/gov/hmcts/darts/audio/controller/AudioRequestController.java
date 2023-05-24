package uk.gov.hmcts.darts.audio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audio.service.AudioRequestService;
import uk.gov.hmcts.darts.audiorequest.api.AudioRequestsApi;
import uk.gov.hmcts.darts.audiorequest.model.AudioRequestDetails;

import javax.validation.Valid;

@RestController
public class AudioRequestController implements AudioRequestsApi {

    @Autowired
    private AudioRequestService audioRequestService;

    /**
     * POST /audio-request/addAudioRequest : Users can request audio for specific cases and date/time periods.
     * Adds a user audio request for processing
     *
     * @param audioRequestDetails Audio Request item to add (optional)
     * @return audio request created (status code 201)
     *         or invalid input, object invalid (status code 400)
     *         or audio request item already exists (status code 409)
     */
    @Operation(
        operationId = "addAudioRequest",
        summary = "Users can request audio for specific cases and date/time periods",
        description = "Adds a user audio request for processing",
        tags = { "Audio Requests" },
        responses = {
            @ApiResponse(responseCode = "201", description = "audio request created"),
            @ApiResponse(responseCode = "400", description = "invalid input, object invalid"),
            @ApiResponse(responseCode = "409", description = "audio request item already exists")
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/audio-request/addAudioRequest",
        consumes = { "application/json" }
    )
    public ResponseEntity<Void> addAudioRequest(
        @Parameter(name = "AudioRequestDetails", description = "Audio Request item to add") @Valid @RequestBody(required = false) AudioRequestDetails audioRequestDetails
    ) {
        try {
            var requestId = audioRequestService.saveAudioRequest(audioRequestDetails);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
