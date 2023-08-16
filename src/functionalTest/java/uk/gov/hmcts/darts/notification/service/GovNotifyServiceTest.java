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
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.CASE_NUMBER;

// These tests are functional in that they connect to the real external gov.uk notify service.
// However, unlike other functional tests these will run against the locally spun up application.
// This is because there is no trigger for sending notifications, such as an API endpoint.

@SpringBootTest
@ActiveProfiles({"dev", "h2db"})
@SuppressWarnings({"PMD.AvoidDuplicateLiterals"})
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
        compare("""
                    Hello,
                    A new Transcription request has been submitted and is awaiting your review.
                    Please visit your DARTS Inbox to action this request.
                    Regards
                    DARTS""", emailResponse);
    }

    private static void compare(String expected, SendEmailResponse emailResponse) {
        String actualUnix = emailResponse.getBody().replace("\r\n", "\n");
        assertEquals(expected, actualUnix);
    }

    private SendEmailResponse createAndSend(String templateName, Map<String, String> parameterMap)
        throws TemplateNotFoundException, NotificationClientException {
        String templateId = templateIdHelper.findTemplateId(templateName);
        GovNotifyRequest govNotifyRequest = new GovNotifyRequest();
        govNotifyRequest.setTemplateId(templateId);
        govNotifyRequest.setEmailAddress(EMAIL_ADDRESS);
        parameterMap.put(CASE_NUMBER, "TheCaseId");
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
        compare("""
                    Hello,
                    A new request has been made for a Transcribed document.
                    Please visit the My Inbox section from within the DARTS portal to access the request.
                    Regards
                    DARTS""", emailResponse);
    }


    @Test
    void requestedAudioIsAvailable() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend("requested_audio_is_available");
        assertEquals("DARTS: Requested Audio is Available", emailResponse.getSubject());
        compare("""
                    Hello,
                    The audio you requested for case TheCaseId is now available.
                    Please visit the My Audio section within DARTS to access your requested Audio.
                    Regards
                    DARTS""", emailResponse);
    }

    @Test
    void transcriptionAvailable() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend("transcription_available");
        assertEquals("DARTS: Transcribed Document Available", emailResponse.getSubject());
        compare("""
                    Hello,
                    The transcript that you requested for case TheCaseId, has now been completed and available for you to view in DARTS.
                    The completed transcribed document can be found under your My Transcriptions Section on the DARTS portal.
                    Regards
                    DARTS""", emailResponse);
    }


    @Test
    void transcriptionRequestApproved() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend("transcription_request_approved");
        assertEquals("DARTS: Transcript Request Approved", emailResponse.getSubject());
        compare("""
                    Hello,
                    The transcript that you requested for case TheCaseId, has now been approved and will be available soon.
                    You will be notified again once the Transcript becomes available on DARTS.
                    Regards
                    DARTS""", emailResponse);
    }


    @Test
    void transcriptionRequestRejected() throws NotificationClientException, TemplateNotFoundException {
        Map<String, String> parameterMap = new ConcurrentHashMap<>();
        parameterMap.put("rejection_reason", "TheRejectionReason");
        SendEmailResponse emailResponse = createAndSend("transcription_request_rejected", parameterMap);
        assertEquals("DARTS: Transcript Request Rejected", emailResponse.getSubject());
        compare("""
                    Hello,
                    The Transcript Request you made for case TheCaseId has been rejected.
                    Rejection Reason: TheRejectionReason
                    If you still need a transcript for this case, please resubmit your request
                    taking into account the rejection reason.
                    Regards
                    DARTS""", emailResponse);
    }

    @Test
    void audioRequestBeingProcessed() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend("audio_request_being_processed");
        assertEquals("DARTS: Audio Request Being Processed", emailResponse.getSubject());
        compare("""
                    Hello,
                    The audio you requested for case TheCaseId is currently being processed.
                    You will be further notified once the audio is available in your My Audio section in DARTS.
                    Regards,
                    DARTS""", emailResponse);
    }

    @Test
    void errorProcessingAudio() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend("error_processing_audio");
        assertEquals("DARTS: Audio Request has Failed", emailResponse.getSubject());
        compare("""
                    Hello,
                    Your audio request for case TheCaseId has failed.Please contact the helpdesk.
                    Regards,
                    DARTS""", emailResponse);

    }

}
