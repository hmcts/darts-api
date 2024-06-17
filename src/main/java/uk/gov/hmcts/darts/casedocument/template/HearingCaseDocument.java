package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class HearingCaseDocument extends CreatedModifiedCaseDocument {

    private Integer id;
    private CourtroomEntity courtroom;
    private LocalDate hearingDate;
    private LocalTime scheduledStartTime;
    private Boolean hearingIsActual;
    private String judgeHearingDate;
    private List<JudgeEntity> judges = new ArrayList<>();
    private List<EventEntity> events = new ArrayList<>();
    private List<MediaEntity> mediaList = new ArrayList<>();
    private List<TranscriptionEntity> transcriptions = new ArrayList<>();
    private List<AnnotationEntity> annotations = new ArrayList<>();
    private List<MediaRequestEntity> mediaRequests = new ArrayList<>();
}
