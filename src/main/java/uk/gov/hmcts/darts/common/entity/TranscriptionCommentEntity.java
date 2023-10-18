package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.time.OffsetDateTime;

@Entity
@Table(name = "transcription_comment")
@Getter
@Setter
public class TranscriptionCommentEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "trc_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trc_gen")
    @SequenceGenerator(name = "trc_gen", sequenceName = "trc_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "trw_id")
    private Integer transcriptionWorkflowId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tra_id")
    private TranscriptionEntity transcription;

    @Column(name = "transcription_object_id", length = 16)
    private String legacyTranscriptionObjectId;

    @Column(name = "transcription_comment")
    private String comment;

    @Column(name = "comment_ts")
    private OffsetDateTime commentTimestamp;

    @Column(name = "author")
    private Integer authorUserId;

}
