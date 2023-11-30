package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transcription_workflow")
@Getter
@Setter
public class TranscriptionWorkflowEntity {

    @Id
    @Column(name = "trw_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trw_gen")
    @SequenceGenerator(name = "trw_gen", sequenceName = "trw_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tra_id", nullable = false)
    private TranscriptionEntity transcription;

    @ManyToOne
    @JoinColumn(name = "trs_id", nullable = false)
    private TranscriptionStatusEntity transcriptionStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_actor", nullable = false)
    private UserAccountEntity workflowActor;

    @Column(name = "workflow_ts", nullable = false)
    private OffsetDateTime workflowTimestamp;

    @OneToMany(mappedBy = TranscriptionCommentEntity_.TRANSCRIPTION_WORKFLOW)
    private List<TranscriptionCommentEntity> transcriptionComments = new ArrayList<>();
}
