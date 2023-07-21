package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
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
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "external_object_directory")
@Getter
@Setter
public class ExternalObjectDirectoryEntity implements JpaAuditing {

    @Id
    @Column(name = "eod_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eod_gen")
    @SequenceGenerator(name = "eod_gen", sequenceName = "eod_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "med_id", foreignKey = @ForeignKey(name = "eod_media_fk"))
    private MediaEntity media;

    @Column(name = "tra_id")
    private Integer transcriptionId;

    @Column(name = "ann_id")
    private Integer annotationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ods_id", foreignKey = @ForeignKey(name = "eod_object_directory_status_fk"), nullable = false)
    private ObjectDirectoryStatusEntity status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elt_id", foreignKey = @ForeignKey(name = "eod_external_location_type_fk"), nullable = false)
    private ExternalLocationTypeEntity externalLocationType;

    @NaturalId
    @Column(name = "external_location", unique = true, nullable = false)
    private UUID externalLocation;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "transfer_attempts")
    private Integer transferAttempts;

    @CreationTimestamp
    @Column(name = "created_ts")
    private OffsetDateTime createdTimestamp;

    @UpdateTimestamp
    @Column(name = "last_modified_ts")
    private OffsetDateTime modifiedTimestamp;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "last_modified_by", foreignKey = @ForeignKey(name = "eod_modified_by_fk"))
    private UserAccount modifiedBy;

}
