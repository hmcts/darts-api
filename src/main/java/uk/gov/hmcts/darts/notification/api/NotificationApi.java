package uk.gov.hmcts.darts.notification.api;

import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;

public interface NotificationApi {

    String getNotificationTemplateIdByName(String templateName) throws TemplateNotFoundException;

    void scheduleNotification(SaveNotificationToDbRequest saveNotificationToDbRequest);

    enum NotificationTemplate {

        ERROR_PROCESSING_AUDIO("error_processing_audio"),
        REQUESTED_AUDIO_AVAILABLE("requested_audio_is_available"),
        AUDIO_REQUEST_PROCESSING("audio_request_being_processed"),
        AUDIO_REQUEST_PROCESSING_ARCHIVE("audio_request_being_processed_from_archive"),
        COURT_MANAGER_APPROVE_TRANSCRIPT("court_manager_approve_transcript"),
        REQUEST_TO_TRANSCRIBER("request_to_transcriber"),
        TRANSCRIPTION_AVAILABLE("transcription_available"),
        TRANSCRIPTION_REQUEST_APPROVED("transcription_request_approved"),
        TRANSCRIPTION_REQUEST_REJECTED("transcription_request_rejected"),
        TRANSCRIPTION_REQUEST_UNFULFILLED("transcription_request_unfulfilled");

        private final String text;

        NotificationTemplate(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
