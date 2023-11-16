package uk.gov.hmcts.darts.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.GovNotifyRequest;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;
import uk.gov.hmcts.darts.notification.helper.TemplateIdHelper;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.CASE_NUMBER;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.PORTAL_URL;

// These tests are functional in that they connect to the real external gov.uk notify service.
// However, unlike other functional tests these will run against the locally spun up application.
// This is because there is no trigger for sending notifications, such as an API endpoint.

@SpringBootTest
@ActiveProfiles({"dev", "h2db"})
@SuppressWarnings({"PMD.LawOfDemeter", "PMD.AvoidDuplicateLiterals"})
class GovNotifyServiceTest {

    public static final String EMAIL_ADDRESS = "test@test.com";
    @Autowired
    GovNotifyService govNotifyService;

    @Autowired
    TemplateIdHelper templateIdHelper;

    @Test
    void courtManagerApproveTranscript() throws NotificationClientException, TemplateNotFoundException {

        SendEmailResponse emailResponse = createAndSend(NotificationApi.NotificationTemplate.COURT_MANAGER_APPROVE_TRANSCRIPT.toString());
        assertEquals("New transcript request submitted and awaiting review", emailResponse.getSubject());
        compare("""
                    There is a new transcript available for you to review.
                    Sign into the DARTS Portal to access it.""", emailResponse);
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
        parameterMap.put(PORTAL_URL, "ThePortalURL");
        govNotifyRequest.setParameterMap(parameterMap);

        return govNotifyService.sendNotification(govNotifyRequest);

    }

    private SendEmailResponse createAndSend(String templateName)
        throws TemplateNotFoundException, NotificationClientException {
        return createAndSend(templateName, new ConcurrentHashMap<>());
    }


    @Test
    void requestToTranscriber() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend(NotificationApi.NotificationTemplate.REQUEST_TO_TRANSCRIBER.toString());
        assertEquals("New transcript request", emailResponse.getSubject());
        compare("""
                    You have received a new transcription request from the DARTS Portal.\s

                    To access the request, please [sign in to the DARTS Portal]""", emailResponse);
    }


    @Test
    void requestedAudioIsAvailable() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend(NotificationApi.NotificationTemplate.REQUESTED_AUDIO_AVAILABLE.toString());
        assertEquals("Your requested audio is available", emailResponse.getSubject());
        compare(
            """
                The audio recording for case number TheCaseId is ready.

                [Sign into the DARTS Portal](ThePortalURL) to access it.

                The recording will expire in 2 working days (this does not include Saturdays and Sundays) but you can extend it by opening the file.""",
            emailResponse
        );
    }

    @Test
    void transcriptionAvailable() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend(NotificationApi.NotificationTemplate.TRANSCRIPTION_AVAILABLE.toString());
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
        SendEmailResponse emailResponse = createAndSend(NotificationApi.NotificationTemplate.TRANSCRIPTION_REQUEST_APPROVED.toString());
        assertEquals("Your transcript request was approved", emailResponse.getSubject());
        compare("""
                    Your transcript request for case ID TheCaseId has been approved.

                    We’ll notify you when it’s available to download.""", emailResponse);
    }


    @Test
    void transcriptionRequestRejected() throws NotificationClientException, TemplateNotFoundException {
        Map<String, String> parameterMap = new ConcurrentHashMap<>();
        parameterMap.put("rejection_reason", "TheRejectionReason");
        SendEmailResponse emailResponse = createAndSend(
            NotificationApi.NotificationTemplate.TRANSCRIPTION_REQUEST_REJECTED.toString(),
            parameterMap
        );
        assertEquals("DARTS: Transcript Request Rejected", emailResponse.getSubject());
        compare(
            """
                Your transcript request for case ID TheCaseId has been rejected due to TheRejectionReason
                Please resubmit your request, taking into account the reason for the original request's rejection.""",
            emailResponse
        );
    }

    @Test
    void audioRequestBeingProcessed() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend(NotificationApi.NotificationTemplate.AUDIO_REQUEST_PROCESSING.toString());
        assertEquals("DARTS has received your audio recording order", emailResponse.getSubject());
        compare("""
                    We have received your audio recording order for case ID TheCaseId, and it's currently being processed.

                    We'll notify you when it's ready and available for use.

                    Alternatively, you can visit the Your audio section in the DARTS Portal to check its progress.""", emailResponse);
    }

    @Test
    void errorProcessingAudio() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend(NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());
        assertEquals("DARTS: Audio Request has Failed", emailResponse.getSubject());
        compare("""
                    Hello,
                    Your audio request for case TheCaseId has failed.Please contact the helpdesk.
                    Regards,
                    DARTS""", emailResponse);

    }

}
