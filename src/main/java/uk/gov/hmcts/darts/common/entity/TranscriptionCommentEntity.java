package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "transcription_comment")
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class TranscriptionCommentEntity extends VersionedEntity {
    @Id
    @Column(name = "trc_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trc_gen")
    @SequenceGenerator(name = "trc_gen", sequenceName = "trc_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
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

    @Column(name = "created_ts")
    private OffsetDateTime createdTimestamp;

    @Column(name = "last_modified_ts")
    private OffsetDateTime lastModifiedTimestamp;

    @Column(name = "last_modified_by")
    private Integer lastModifiedByUserId;

    @Column(name = "superseded")
    private Boolean superseded;

}
