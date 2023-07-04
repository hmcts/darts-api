package uk.gov.hmcts.darts.notification.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles({"intTest", "postgresTestContainer"})
class TemplateIdHelperTest {

    @Autowired
    TemplateIdHelper templateIdHelper;

    @Test
    void okCourtManagerApproveTranscript() throws TemplateNotFoundException {
        String templateId = templateIdHelper.findTemplateId("court_manager_approve_transcript");
        assertEquals("2f9e18b5-5046-44c2-8abd-e4ff15008a60", templateId);
    }

    @Test
    void okRequestedAudioIsAvailable() throws TemplateNotFoundException {
        String templateId = templateIdHelper.findTemplateId("requested_audio_is_available");
        assertEquals("66a1864f-24a6-469a-ac55-66bc57c7e4f6", templateId);
    }

    @Test
    void okRequestToTranscriber() throws TemplateNotFoundException {
        String templateId = templateIdHelper.findTemplateId("request_to_transcriber");
        assertEquals("976bf288-705d-4cbb-b24f-c5529abf14cf", templateId);
    }

    @Test
    void notFound() throws TemplateNotFoundException {

        TemplateNotFoundException thrown = Assertions.assertThrows(TemplateNotFoundException.class, () -> {
            templateIdHelper.findTemplateId("test");
        });


        assertEquals("Unable to find template with name 'test'.", thrown.getMessage());
    }

}
