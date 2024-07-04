package uk.gov.hmcts.darts.casedocument.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class HearingCaseDocument extends CreatedModifiedCaseDocument {

    private final Integer id;
    private final CourtroomCaseDocument courtroom;
    private final LocalDate hearingDate;
    private final LocalTime scheduledStartTime;
    private final Boolean hearingIsActual;
    private final String judgeHearingDate;
    private final List<JudgeCaseDocument> judges;
    private final List<EventCaseDocument> events;
    private final List<MediaRequestCaseDocument> mediaRequests;
    private final List<MediaCaseDocument> medias;
    private final List<TranscriptionCaseDocument> transcriptions;
    private final List<AnnotationCaseDocument> annotations;
}
