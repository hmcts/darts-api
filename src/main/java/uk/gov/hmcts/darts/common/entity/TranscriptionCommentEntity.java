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
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.util.DataUtil;

import java.time.OffsetDateTime;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

@Entity
@Table(name = "transcription_comment")
@Getter
@Setter
@Audited
@AuditTable("transcription_comment_aud")
public class TranscriptionCommentEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "trc_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trc_gen")
    @SequenceGenerator(name = "trc_gen", sequenceName = "trc_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Audited(targetAuditMode = NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trw_id")
    private TranscriptionWorkflowEntity transcriptionWorkflow;

    @Audited(targetAuditMode = NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tra_id", nullable = false)
    private TranscriptionEntity transcription;

    @Column(name = "transcription_object_id", length = 16)
    private String legacyTranscriptionObjectId;

    @Column(name = "transcription_comment")
    private String comment;

    @Column(name = "comment_ts")
    private OffsetDateTime commentTimestamp;

    @Column(name = "author")
    private Integer authorUserId;

    @NotAudited
    @Column(name = "is_migrated")
    private boolean isMigrated;

    @Column(name = "is_data_anonymised")
    private boolean isDataAnonymised;
}
