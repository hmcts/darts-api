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
import uk.gov.hmcts.darts.common.entity.base.ModifiedBaseEntity;

import java.time.OffsetDateTime;

@Entity
@Table(name = CaseDocumentEntity.TABLE_NAME)
@Getter
@Setter
public class CaseDocumentEntity extends ModifiedBaseEntity {

    public static final String ID = "cad_id";
    public static final String TABLE_NAME = "case_document";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cad_gen")
    @SequenceGenerator(name = "cad_gen", sequenceName = "cad_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ohr_id")
    private ObjectHiddenReasonEntity objectHiddenReason;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Integer fileSize;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "is_hidden", nullable = false)
    private boolean hidden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hidden_by")
    private UserAccountEntity hiddenBy;

    @Column(name = "hidden_ts")
    private OffsetDateTime hiddenTs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private UserAccountEntity createdBy;

    @Column(name = "created_ts", nullable = false)
    private OffsetDateTime createdTs;

    @Column(name = "marked_for_manual_deletion", nullable = false)
    private boolean markedForManualDeletion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_for_manual_del_by")
    private UserAccountEntity markedForManualDelBy;

    @Column(name = "marked_for_manual_del_ts")
    private OffsetDateTime markedForManualDelTs;

    @Column(name = "retain_until_ts")
    private OffsetDateTime retainUntilTs;

}
