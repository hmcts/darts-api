package uk.gov.hmcts.darts.audio.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import uk.gov.hmcts.darts.audio.service.impl.AudioTransformationServiceImpl;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SystemCommandExecutorStubImpl;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.FAILED;


@Import(SystemCommandExecutorStubImpl.class)
@ExtendWith(MockitoExtension.class)
class AudioTransformationServiceProcessAudioRequestTest extends IntegrationBase {

    @Autowired
    private AudioTransformationServiceProcessAudioRequestGivenBuilder given;

    @Autowired
    private AudioTransformationServiceImpl audioTransformationService;

    @BeforeEach
    void setUp() {
        var hearing = given.aHearingWith("1", "some-courthouse", "some-courtroom");
        given.aMediaRequestEntityFor(hearing);
    }

    @Test
    void processAudioRequestShouldSucceedAndUpdateRequestStatusToCompleted() {
        given.databaseIsProvisionedForHappyPath();

        Integer mediaRequestId = given.getMediaRequestEntity().getId();

        UUID blobId = audioTransformationService.processAudioRequest(mediaRequestId);
        assertNotNull(blobId);

        var mediaRequestEntity = dartsDatabase.getMediaRequestRepository()
              .findById(mediaRequestId)
              .orElseThrow();

        assertEquals(COMPLETED, mediaRequestEntity.getStatus());
    }

    @Test
    void processAudioRequestShouldFailAndUpdateRequestStatusToFailed() {
        Integer mediaRequestId = given.getMediaRequestEntity().getId();
        var exception = assertThrows(
              DartsApiException.class,
              () -> audioTransformationService.processAudioRequest(mediaRequestId)
        );

        assertEquals("Failed to process audio request", exception.getMessage());

        var mediaRequestEntity = dartsDatabase.getMediaRequestRepository()
              .findById(mediaRequestId)
              .orElseThrow();
        assertEquals(FAILED, mediaRequestEntity.getStatus());
    }
}
