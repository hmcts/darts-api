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
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.AUDIO_END_TIME;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.AUDIO_START_TIME;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.CASE_NUMBER;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.COURTHOUSE;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.DEFENDANTS;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.HEARING_DATE;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.PORTAL_URL;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.REQUEST_ID;

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
        assertEquals("A new transcript is ready for you to review", emailResponse.getSubject());
        compare("""
                    There is a new transcript available for you to review.

                    [Sign into the DARTS Portal](ThePortalURL) to access it.""", emailResponse);
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
        parameterMap.put(COURTHOUSE, "TheCourthouse");
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
        assertEquals("New DARTS transcription request", emailResponse.getSubject());
        compare("""
                    You have received a new transcription request from the DARTS Portal.

                    [Sign into the DARTS Portal](ThePortalURL) to access it.""", emailResponse);
    }


    @Test
    void requestedAudioIsAvailable() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend(NotificationApi.NotificationTemplate.REQUESTED_AUDIO_AVAILABLE.toString());
        assertEquals("Your requested audio is available", emailResponse.getSubject());
        compare(
            """
                The audio recording for case ID TheCaseId is ready.

                [Sign into the DARTS Portal](ThePortalURL) to access it.

                The recording will expire in 2 working days (this does not include Saturdays and Sundays) but you can extend it by opening the file.""",
            emailResponse
        );
    }

    @Test
    void transcriptionAvailable() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend(NotificationApi.NotificationTemplate.TRANSCRIPTION_AVAILABLE.toString());
        assertEquals("Your transcript is available", emailResponse.getSubject());
        compare("""
                    Your transcript request for case ID TheCaseId has been completed and is available for download.

                    To access the transcript, [Sign into the DARTS Portal](ThePortalURL) and go to ‘Your transcripts’.""", emailResponse);
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
        assertEquals("Your transcript request was rejected", emailResponse.getSubject());
        compare(
            """
                Your transcript request for case ID TheCaseId has been rejected due to TheRejectionReason.

                You can resubmit your request, but take into account the reason for the original request's rejection.""",
            emailResponse
        );
    }

    @Test
    void audioRequestBeingProcessed() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend(NotificationApi.NotificationTemplate.AUDIO_REQUEST_PROCESSING.toString());
        assertEquals("DARTS has received your audio recording order", emailResponse.getSubject());
        compare("""
                    We have received your audio recording order for case ID TheCaseId at TheCourthouse and it's currently being processed.

                    We'll notify you when it's ready and available for use.

                    Alternatively, you can visit the Your audio section in the DARTS Portal to check its progress.""", emailResponse);
    }

    @Test
    void audioRequestBeingProcessedFromArchive() throws NotificationClientException, TemplateNotFoundException {
        SendEmailResponse emailResponse = createAndSend(NotificationApi.NotificationTemplate.AUDIO_REQUEST_PROCESSING_ARCHIVE.toString());
        assertEquals("DARTS has received your audio recording order", emailResponse.getSubject());
        compare("""
                    We have received your audio recording order for case ID TheCaseId.

                    Processing your order may take a little longer as it must be retrieved from the archives.

                    We'll notify you when it is ready and available for use.

                    Alternatively, you can visit the Your audio section of the DARTS Portal to check its progress.""", emailResponse);
    }

    @Test
    void errorProcessingAudio() throws NotificationClientException, TemplateNotFoundException {
        Map<String, String> parameterMap = new ConcurrentHashMap<>();
        parameterMap.put(REQUEST_ID, "TheRequestID");
        parameterMap.put(COURTHOUSE, "TheCourthouse");
        parameterMap.put(DEFENDANTS, "Defendant1,Defendant2");
        parameterMap.put(HEARING_DATE, "TheHearingDate");
        parameterMap.put(AUDIO_START_TIME, "TheStartTime");
        parameterMap.put(AUDIO_END_TIME, "TheEndTime");
        SendEmailResponse emailResponse = createAndSend(
            NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString(),
            parameterMap
        );
        assertEquals("Your audio recording order has failed", emailResponse.getSubject());
        compare(
            """
                Your audio recording order for case ID TheCaseId has failed.

                Due to unforeseen errors, your audio recording order has failed.

                To resolve this issue, email DTS-ITServiceDesk@justice.gov.uk quoting TheRequestID, and provide them with the following information:

                ## Case details

                Case ID: TheCaseId
                Courthouse: TheCourthouse
                Defendants: Defendant1,Defendant2

                ## Audio details

                Hearing date: TheHearingDate
                Requested start time: TheStartTime
                Requested end time: TheEndTime

                They will raise a Service Now ticket to process this issue.""",
            emailResponse
        );
    }
}
