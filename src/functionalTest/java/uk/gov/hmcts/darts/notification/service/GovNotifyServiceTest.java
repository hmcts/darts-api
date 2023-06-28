package uk.gov.hmcts.darts.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.notification.dto.GovNotifyRequest;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;
import uk.gov.hmcts.darts.notification.helper.TemplateIdHelper;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

// These tests are functional in that they connect to the real external gov.uk notify service.
// However, unlike other functional tests these will run against the locally spun up application.
// This is because there is no trigger for sending notifications, such as an API endpoint.

@SpringBootTest
@ActiveProfiles({"dev","h2db"})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class GovNotifyServiceTest {

    public static final String EMAIL_ADDRESS = "test@test.com";
    @Autowired
    GovNotifyService govNotifyService;

    @Autowired
    TemplateIdHelper templateIdHelper;

    @Test
    void courtManagerApproveTranscript() throws NotificationClientException, TemplateNotFoundException {

        SendEmailResponse emailResponse = createAndSend("court_manager_approve_transcript");
        assertEquals("DARTS: New Transcription Request Submitted and Awaiting Review", emailResponse.getSubject());
        assertEquals("Hello,\r\n" +
                         "A new Transcription request has been submitted and is awaiting your review. \r\n" +
                         "Please visit your DARTS Inbox to action this request. \r\n" +
                         "Regards\r\n" +
                         "DARTS", emailResponse.getBody());
    }

    private SendEmailResponse createAndSend(String templateName, Map<String, String> parameterMap)
        throws TemplateNotFoundException, NotificationClientException {
        String templateId = templateIdHelper.findTemplateId(templateName);
        GovNotifyRequest govNotifyRequest = new GovNotifyRequest();
        govNotifyRequest.setTemplateId(templateId);
        govNotifyRequest.setEmailAddress(EMAIL_ADDRESS);
        parameterMap.put("case_id", "TheCaseId");
        govNotifyRequest.setParameterMap(parameterMap);

        return govNotifyService.sendNotification(govNotifyRequest);

    }

    private SendEmailResponse createAndSend(String templateName)
        throws TemplateNotFoundException, NotificationClientException {
        return createAndSend(templateName, new ConcurrentHashMap<>());
    }


    @Test
    void requestToTranscriber() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend("request_to_transcriber");
        assertEquals("DARTS: New Transcription Request", emailResponse.getSubject());
        assertEquals("Hello,\r\n" +
                         "A new request has been made for a Transcribed document.\r\n" +
                         "Please visit the My Inbox section from within the DARTS portal to access the request.\r\n" +
                         "Regards\r\n" +
                         "DARTS", emailResponse.getBody());
    }


    @Test
    void requestedAudioIsAvailable() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend("requested_audio_is_available");
        assertEquals("DARTS: Requested Audio is Available", emailResponse.getSubject());
        assertEquals("Hello,\r\n" +
                         "The audio you requested for case TheCaseId is now available.\r\n" +
                         "Please visit the My Audio section within DARTS to access your requested Audio.\r\n" +
                         "Regards\r\n" +
                         "DARTS", emailResponse.getBody());
    }

    @Test
    void transcriptionAvailable() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend("transcription_available");
        assertEquals("DARTS: Transcribed Document Available", emailResponse.getSubject());
        assertEquals("Hello,\r\n" +
                         "The transcript that you requested for case TheCaseId, has now been completed and available for you to view in DARTS.\r\n" +
                         "The completed transcribed document can be found under your My Transcriptions Section on the DARTS portal.\r\n" +
                         "Regards\r\n" +
                         "DARTS", emailResponse.getBody());
    }


    @Test
    void transcriptionRequestApproved() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend("transcription_request_approved");
        assertEquals("DARTS: Transcript Request Approved", emailResponse.getSubject());
        assertEquals("Hello,\r\n" +
                         "The transcript that you requested for case TheCaseId, has now been approved and will be available soon. " +
                         "You will be notified again once the Transcript becomes available on DARTS.\r\n" +
                         "Regards\r\n" +
                         "DARTS", emailResponse.getBody());
    }


    @Test
    void transcriptionRequestRejected() throws NotificationClientException, TemplateNotFoundException {
        Map<String, String> parameterMap = new ConcurrentHashMap<>();
        parameterMap.put("rejection_reason", "TheRejectionReason");
        SendEmailResponse emailResponse = createAndSend("transcription_request_rejected", parameterMap);
        assertEquals("DARTS: Transcript Request Rejected", emailResponse.getSubject());
        assertEquals("Hello,\r\n" +
                         "The Transcript Request you made for case TheCaseId has been rejected.\r\n" +
                         "Rejection Reason: TheRejectionReason\r\n" +
                         "If you still need a transcript for this case, please resubmit your request\r\n" +
                         "taking into account the rejection reason.\r\n" +
                         "Regards\r\n" +
                         "DARTS", emailResponse.getBody());
    }

    @Test
    void audioRequestBeingProcessed() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend("audio_request_being_processed");
        assertEquals("DARTS: Audio Request Being Processed", emailResponse.getSubject());
        assertEquals("Hello,\r\n" +
                         "The audio you requested for case TheCaseId is currently being processed.\r\n" +
                         "\r\n" +
                         "You will be further notified once the audio is available in your My Audio section in DARTS.\r\n" +
                         "\r\n" +
                         "Regards, \r\n" +
                         "\r\n" +
                         "DARTS", emailResponse.getBody());
    }

    @Test
    void errorProcessingAudio() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend("error_processing_audio");
        assertEquals("DARTS: Audio Request has Failed", emailResponse.getSubject());
        assertEquals("Hello,\r\n" +
                         "Your audio request for case TheCaseId has failed.Please contact the helpdesk.\r\n" +
                         "\r\n" +
                         "Regards, \r\n" +
                         "\r\n" +
                         "DARTS", emailResponse.getBody());
    }

}
