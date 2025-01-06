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
        assertEquals("a8390fa6-3f18-44c0-b224-f59971a5e20a", templateId);
    }

    @Test
    void okRequestToTranscriber() throws TemplateNotFoundException {
        String templateId = templateIdHelper.findTemplateId("request_to_transcriber");
        assertEquals("12a70a9c-9bcf-4880-8291-1a5c6a4c4b08", templateId);
    }

    @Test
    void notFound() throws TemplateNotFoundException {

        TemplateNotFoundException thrown = Assertions.assertThrows(TemplateNotFoundException.class, () -> {
            templateIdHelper.findTemplateId("test");
        });


        assertEquals("Unable to find template with name 'test'.", thrown.getMessage());
    }

}
