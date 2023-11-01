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
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "transcription_document")
@Getter
@Setter
public class TranscriptionDocumentEntity {
    @Id
    @Column(name = "trd_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trd_gen")
    @SequenceGenerator(name = "trd_gen", sequenceName = "trd_seq", allocationSize = 1)
    private Integer id;


    @Column(name = "tra_id", nullable = false)
    private Integer transcriptionId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private UserAccountEntity uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_ts")
    private OffsetDateTime uploadedDateTime;

}
