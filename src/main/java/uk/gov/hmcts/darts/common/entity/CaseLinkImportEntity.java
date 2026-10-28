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
@Table(name = "case_link_import")
@Getter
@Setter
public class CaseLinkImportEntity extends CreatedModifiedBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cli_gen")
    @SequenceGenerator(name = "cli_gen", sequenceName = "cli_seq", allocationSize = 1)
    @Column(name = "cli_id")
    private Long id;

    @Column(name = "original_filename", columnDefinition = "text")
    private String originalFilename;

    @Column(name = "file_checksum", length = 64)
    private String fileChecksum;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cis_id", nullable = false)
    private CaseLinkImportStatusEntity status;

    @Column(name = "processing_started_ts")
    private OffsetDateTime processingStartedAt;

    @Column(name = "processing_completed_ts")
    private OffsetDateTime processingCompletedAt;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
}
