package uk.gov.hmcts.darts.notification.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;

import java.text.MessageFormat;


@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateIdHelper {

    @Value("${darts.notification.gov_notify.template.transcriber.court_manager_approve_transcript}")
    private String courtManagerApproveTranscript;

    @Value("${darts.notification.gov_notify.template.transcriber.request_to_transcriber}")
    private String requestToTranscriber;

    @Value("${darts.notification.gov_notify.template.transcriber.requested_audio_is_available}")
    private String requestedAudioIsAvailable;

    public String findTemplateId(String templateName) throws TemplateNotFoundException {
        switch (templateName) {
            case "court_manager_approve_transcript" : return courtManagerApproveTranscript;
            case "request_to_transcriber" : return requestToTranscriber;
            case "requested_audio_is_available" : return requestedAudioIsAvailable;
            default:
                String errorMessage = MessageFormat.format("Unable to find template with name ''{0}''.", templateName);
                log.error(errorMessage);
                throw new TemplateNotFoundException(errorMessage);
        }
    }

}
