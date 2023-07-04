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
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Entity
@Table(name = "moj_transcription_comment")
@Data
@EqualsAndHashCode(callSuper = false)
public class TranscriptionCommentEntity extends VersionedEntity {

    @Id
    @Column(name = "moj_trc_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_trc_gen")
    @SequenceGenerator(name = "moj_trc_gen", sequenceName = "moj_trc_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moj_tra_id")
    private TranscriptionEntity transcription;

    @Column(name = "r_transcription_object_id", length = 16)
    private String legacyTranscriptionObjectId;

    @Column(name = "c_comment")
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

    @Column(name = "i_superseded")
    private Boolean superseded;

}
