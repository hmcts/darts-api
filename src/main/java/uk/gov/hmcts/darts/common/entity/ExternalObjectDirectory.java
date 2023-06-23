package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = ExternalObjectDirectory.TABLE_NAME)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExternalObjectDirectory {

    public static final String ID = "eod_id";
    public static final String MEDIA_ID = "moj_med_id";
    public static final String TRANSCRIPTION_ID = "moj_tra_id";
    public static final String ANNOTATION_ID = "moj_ann_id";
    public static final String EXTERNAL_LOCATION = "external_location";
    public static final String EXTERNAL_LOCATION_TYPE = "external_location_type";
    public static final String CREATED_DATE_TIME = "created_ts";
    public static final String LAST_UPDATED_DATE_TIME = "modified_ts";
    public static final String MODIFIED_BY = "modified_by";
    public static final String STATUS = "status";
    public static final String CHECKSUM = "checksum";
    public static final String ATTEMPTS = "attempts";
    public static final String TABLE_NAME = "external_object_directory";

    @Id
    @Column(name = ID)
    private Integer id;

    @Column(name = MEDIA_ID)
    private Integer mediaId;

    @Column(name = TRANSCRIPTION_ID)
    private Integer transcriptionId;

    @Column(name = ANNOTATION_ID)
    private Integer annotationId;

    @Column(name = EXTERNAL_LOCATION)
    private String externalLocation;

    @Column(name = EXTERNAL_LOCATION_TYPE)
    private String externalLocationType;

    @CreationTimestamp
    @Column(name = CREATED_DATE_TIME)
    private OffsetDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = LAST_UPDATED_DATE_TIME)
    private OffsetDateTime lastUpdatedDateTime;

    @Column(name = MODIFIED_BY)
    private Integer modifiedBy;

    @Column(name = STATUS)
    private Integer status;

    @Column(name = CHECKSUM)
    private String checksum;

    @Column(name = ATTEMPTS)
    private Integer attempts;

}
