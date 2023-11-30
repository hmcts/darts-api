package uk.gov.hmcts.darts.notification.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
class TemplateIdHelperTest {

    @Autowired
    TemplateIdHelper templateIdHelper;

    @Test
    void okCourtManagerApproveTranscript() throws TemplateNotFoundException {
        String templateId = templateIdHelper.findTemplateId("court_manager_approve_transcript");
        assertEquals("a8390fa6-3f18-44c0-b224-f59971a5e20a", templateId);
    }

    @Test
    void okRequestedAudioIsAvailable() throws TemplateNotFoundException {
        String templateId = templateIdHelper.findTemplateId("requested_audio_is_available");
        assertEquals("5038c158-f7c9-4781-837e-3aaae8e906ed", templateId);
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
