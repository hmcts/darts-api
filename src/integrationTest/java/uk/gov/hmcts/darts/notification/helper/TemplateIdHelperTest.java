package uk.gov.hmcts.darts.notification.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateIdHelperTest extends IntegrationBase {

    @Autowired
    TemplateIdHelper templateIdHelper;

    @Test
    void okAudioRequestBeingProcessed() throws TemplateNotFoundException {
        String templateId = templateIdHelper.findTemplateId("audio_request_being_processed");
        assertEquals("bbc5ffc0-8f75-412a-9f01-a99ef3e498da", templateId);
    }

    @Test
    void okAudioRequestBeingProcessedFromArchive() throws TemplateNotFoundException {
        String templateId = templateIdHelper.findTemplateId("audio_request_being_processed_from_archive");
        assertEquals("c616d920-e36a-489b-af74-4ea068e967f9", templateId);
    }

    @Test
    void okErrorProcessingAudio() throws TemplateNotFoundException {
        String templateId = templateIdHelper.findTemplateId("error_processing_audio");
        assertEquals("10d7e02d-360c-47b7-a97c-a98c13f3c122", templateId);
    }

    @Test
    void okRequestedAudioIsAvailable() throws TemplateNotFoundException {
        String templateId = templateIdHelper.findTemplateId("requested_audio_is_available");
        assertEquals("a6747899-08e9-4cbf-86d3-a3b871a66e86", templateId);
    }

    @Test
    void okCourtManagerApproveTranscript() throws TemplateNotFoundException {
        String templateId = templateIdHelper.findTemplateId("court_manager_approve_transcript");
        assertEquals("4bf3fbbb-4474-46c9-9f30-499f9083653a", templateId);
    }

    @Test
    void okRequestToTranscriber() throws TemplateNotFoundException {
        String templateId = templateIdHelper.findTemplateId("request_to_transcriber");
        assertEquals("463e63c5-9521-46c7-a0f0-5c40ad7a2240", templateId);
    }

    @Test
    void okRequestTranscriptionAvailable() throws TemplateNotFoundException {
        String templateId = templateIdHelper.findTemplateId("transcription_available");
        assertEquals("96d3b32c-6a6a-45ae-8c0e-91e9d04d2eb0", templateId);
    }

    @Test
    void okRequestTranscriptionRequestApproved() throws TemplateNotFoundException {
        String templateId = templateIdHelper.findTemplateId("transcription_request_approved");
        assertEquals("2e0a929b-6939-4d26-99d2-01cdb7454065", templateId);
    }

    @Test
    void okRequestTranscriptionRequestRejected() throws TemplateNotFoundException {
        String templateId = templateIdHelper.findTemplateId("transcription_request_rejected");
        assertEquals("9b48d057-5dda-4e36-8727-81a01e3a39ec", templateId);
    }

    @Test
    void notFound() throws TemplateNotFoundException {

        TemplateNotFoundException thrown = Assertions.assertThrows(TemplateNotFoundException.class, () -> {
            templateIdHelper.findTemplateId("test");
        });


        assertEquals("Unable to find template with name 'test'.", thrown.getMessage());
    }

}
