package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

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

    @ManyToOne
    @JoinColumn(name = "cas_id", nullable = false)
    private CourtCaseEntity courtCase;

    @ManyToOne
    @JoinColumn(name = "trt_id", nullable = false)
    private TranscriptionTypeEntity transcriptionType;

    @ManyToOne
    @JoinColumn(name = "ctr_id")
    private CourtroomEntity courtroom;

    @ManyToOne
    @JoinColumn(name = "tru_id")
    private TranscriptionUrgencyEntity transcriptionUrgency;

    @ManyToOne
    @JoinColumn(name = "hea_id")
    private HearingEntity hearing;

    @ManyToOne
    @JoinColumn(name = "trs_id", nullable = false)
    private TranscriptionStatusEntity transcriptionStatus;

    @Column(name = "transcription_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "requestor")
    private String requestor;

    @Column(name = "hearing_date")
    private OffsetDateTime hearingDate;

    @Column(name = "start_ts")
    private OffsetDateTime startTime;

    @Column(name = "end_ts")
    private OffsetDateTime endTime;

    @Column(name = "version_label", length = 32)
    private String legacyVersionLabel;

    @OneToMany(mappedBy = TranscriptionCommentEntity_.TRANSCRIPTION)
    private List<TranscriptionCommentEntity> transcriptionCommentEntities = new ArrayList<>();

    @OneToMany(cascade = {PERSIST, MERGE}, mappedBy = TranscriptionWorkflowEntity_.TRANSCRIPTION)
    private List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = new ArrayList<>();

    @OneToMany(mappedBy = TranscriptionDocumentEntity_.TRANSCRIPTION)
    private List<TranscriptionDocumentEntity> transcriptionDocumentEntities = new ArrayList<>();

}
