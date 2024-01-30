package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;

@Entity
@Table(name = "transcription")
@Getter
@Setter
public class TranscriptionEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "tra_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tra_gen")
    @SequenceGenerator(name = "tra_gen", sequenceName = "tra_seq", allocationSize = 1)
    private Integer id;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "case_transcription_ae",
        joinColumns = {@JoinColumn(name = "tra_id")},
        inverseJoinColumns = {@JoinColumn(name = "cas_id")})
    private List<CourtCaseEntity> courtCases = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "trt_id", nullable = false)
    private TranscriptionTypeEntity transcriptionType;

    @ManyToOne
    @JoinColumn(name = "ctr_id")
    private CourtroomEntity courtroom;

    @ManyToOne
    @JoinColumn(name = "tru_id")
    private TranscriptionUrgencyEntity transcriptionUrgency;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "hearing_transcription_ae",
        joinColumns = {@JoinColumn(name = "tra_id")},
        inverseJoinColumns = {@JoinColumn(name = "hea_id")})
    private List<HearingEntity> hearings = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "trs_id")
    private TranscriptionStatusEntity transcriptionStatus;

    @Column(name = "transcription_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "requestor")
    private String requestor;

    @Column(name = "hearing_date")
    private LocalDate hearingDate;

    @Column(name = "start_ts")
    private OffsetDateTime startTime;

    @Column(name = "end_ts")
    private OffsetDateTime endTime;

    @Column(name = "version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "is_manual_transcription", nullable = false)
    private Boolean isManualTranscription;

    @Column(name = "hide_request_from_requestor", nullable = false)
    private Boolean hideRequestFromRequestor;

    @Column(name = "chronicle_id")
    private String chronicleId;

    @Column(name = "antecedent_id")
    private String antecedentId;

    @OneToMany(mappedBy = TranscriptionCommentEntity_.TRANSCRIPTION)
    private List<TranscriptionCommentEntity> transcriptionCommentEntities = new ArrayList<>();

    @OneToMany(cascade = {PERSIST, MERGE}, mappedBy = TranscriptionWorkflowEntity_.TRANSCRIPTION)
    private List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = new ArrayList<>();

    @OneToMany(mappedBy = TranscriptionDocumentEntity_.TRANSCRIPTION)
    private List<TranscriptionDocumentEntity> transcriptionDocumentEntities = new ArrayList<>();

    public void addCase(CourtCaseEntity courtCase) {
        if (courtCase != null) {
            courtCases.add(courtCase);
        }
    }

    public void addHearing(HearingEntity hearing) {
        if (hearing != null) {
            addCase(hearing.getCourtCase());
            hearings.add(hearing);
        }
    }

    /*
        courtCases should be looped through
     */
    @Deprecated
    public CourtCaseEntity getCourtCase() {
        if (CollectionUtils.isEmpty(courtCases)) {
            HearingEntity hearing = getHearing();
            if (hearing == null) {
                return null;
            } else {
                return hearing.getCourtCase();
            }
        }
        return courtCases.get(0);
    }

    public HearingEntity getHearing() {
        if (CollectionUtils.isEmpty(hearings)) {
            return null;
        }
        return hearings.get(0);
    }
}
