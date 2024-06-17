package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class HearingCaseDocument extends CreatedModifiedCaseDocument {

    private Integer id;
    private CourtroomCaseDocument courtroom;
    private LocalDate hearingDate;
    private LocalTime scheduledStartTime;
    private Boolean hearingIsActual;
    private String judgeHearingDate;
    private List<JudgeCaseDocument> judges;
    private List<EventCaseDocument> events;
    private List<MediaRequestCaseDocument> mediaRequests;
    private List<MediaCaseDocument> medias;
//    private List<TranscriptionEntity> transcriptions;
//    private List<AnnotationEntity> annotations;
}
