package uk.gov.hmcts.darts.common.entity;

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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = ExternalObjectDirectoryEntity.TABLE_NAME)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExternalObjectDirectoryEntity implements JpaAuditing {

    public static final String ID = "eod_id";
    public static final String MEDIA_ID = "moj_med_id";
    public static final String TRANSCRIPTION_ID = "moj_tra_id";
    public static final String ANNOTATION_ID = "moj_ann_id";
    public static final String EXTERNAL_LOCATION = "external_location";
    public static final String EXTERNAL_LOCATION_TYPE = "external_location_type";
    public static final String CREATED_TIMESTAMP = "created_ts";
    public static final String MODIFIED_TIMESTAMP = "modified_ts";
    public static final String MODIFIED_BY = "modified_by";
    public static final String STATUS_ID = "moj_ods_id";
    public static final String CHECKSUM = "checksum";
    public static final String ATTEMPTS = "attempts";
    public static final String TABLE_NAME = "external_object_directory";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eod_gen")
    @SequenceGenerator(name = "eod_gen", sequenceName = "eod_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = MEDIA_ID, foreignKey = @ForeignKey(name = "eod_media_fk"))
    private MediaEntity media;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = STATUS_ID, foreignKey = @ForeignKey(name = "eod_object_directory_status_fk"))
    private ObjectDirectoryStatusEntity status;

    @Column(name = TRANSCRIPTION_ID)
    private Integer transcriptionId;

    @Column(name = ANNOTATION_ID)
    private Integer annotationId;

    @NaturalId
    @Column(name = EXTERNAL_LOCATION, unique = true, nullable = false)
    private UUID externalLocation;

    @Column(name = EXTERNAL_LOCATION_TYPE)
    private String externalLocationType;

    @CreationTimestamp
    @Column(name = CREATED_TIMESTAMP)
    private OffsetDateTime createdTimestamp;

    @UpdateTimestamp
    @Column(name = MODIFIED_TIMESTAMP)
    private OffsetDateTime modifiedTimestamp;

    @Column(name = MODIFIED_BY)
    private Integer modifiedBy;

    @Column(name = CHECKSUM)
    private String checksum;

    @Column(name = ATTEMPTS)
    private Integer attempts;

}
