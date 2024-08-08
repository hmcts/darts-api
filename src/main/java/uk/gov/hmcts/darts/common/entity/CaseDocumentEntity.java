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
@Table(name = CaseDocumentEntity.TABLE_NAME)
@Getter
@Setter
public class CaseDocumentEntity extends CreatedModifiedBaseEntity implements ConfidenceAware {

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

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Integer fileSize;

    @Column(name = "is_deleted")
    private boolean isDeleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private UserAccountEntity deletedBy;

    @Column(name = "deleted_ts")
    private OffsetDateTime deletedTs;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "is_hidden", nullable = false)
    private boolean hidden;

    @Column(name = "retain_until_ts")
    private OffsetDateTime retainUntilTs;

    @Column(name = "ret_conf_score")
    private Integer retConfScore;

    @Column(name = "ret_conf_reason")
    private String retConfReason;

}