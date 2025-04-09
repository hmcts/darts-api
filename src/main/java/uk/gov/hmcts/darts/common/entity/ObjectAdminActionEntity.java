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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.util.DataUtil;

import java.time.OffsetDateTime;

@Entity
@Table(name = "object_admin_action")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ObjectAdminActionEntity {

    @Id
    @Column(name = "oaa_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "oaa_gen")
    @SequenceGenerator(name = "oaa_gen", sequenceName = "oaa_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "ado_id")
    private AnnotationDocumentEntity annotationDocument;

    @ManyToOne
    @JoinColumn(name = "cad_id")
    private CaseDocumentEntity caseDocument;

    @ManyToOne
    @JoinColumn(name = "med_id")
    private MediaEntity media;

    @ManyToOne
    @JoinColumn(name = "trd_id")
    private TranscriptionDocumentEntity transcriptionDocument;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ohr_id")
    private ObjectHiddenReasonEntity objectHiddenReason;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hidden_by")
    private UserAccountEntity hiddenBy;

    @Column(name = "hidden_ts")
    private OffsetDateTime hiddenDateTime;

    @Column(name = "marked_for_manual_deletion")
    private boolean markedForManualDeletion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "marked_for_manual_del_by")
    private UserAccountEntity markedForManualDelBy;

    @Column(name = "marked_for_manual_del_ts")
    private OffsetDateTime markedForManualDelDateTime;

    @Column(name = "ticket_reference")
    private String ticketReference;

    @Column(name = "comments")
    private String comments;

}
