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
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.util.DataUtil;

import java.time.OffsetDateTime;

@Entity
@Table(name = "object_retrieval_queue")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ObjectRetrievalQueueEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "orq_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orq_gen")
    @SequenceGenerator(name = "orq_gen", sequenceName = "orq_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "med_id")
    private MediaEntity media;

    @ManyToOne
    @JoinColumn(name = "trd_id")
    private TranscriptionDocumentEntity transcriptionDocument;

    @Column(name = "parent_object_id")
    private String parentObjectId;

    @Column(name = "content_object_id")
    private String contentObjectId;

    @Column(name = "clip_id")
    private String clipId;

    @Column(name = "acknowledged_ts")
    private OffsetDateTime acknowledgedTs;

    @Column(name = "migrated_ts")
    private OffsetDateTime migratedTs;

    @Column(name = "status")
    private String status;

    @Column(name = "data_ticket")
    private Integer dataTicket;

    @Column(name = "storage_id", length = 16)
    private String storageId;

}
