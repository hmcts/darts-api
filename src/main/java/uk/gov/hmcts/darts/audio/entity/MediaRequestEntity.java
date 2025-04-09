package uk.gov.hmcts.darts.audio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity_;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.util.DataUtil;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = MediaRequestEntity.TABLE_NAME)
@Getter
@Setter
@AuditTable("media_request_aud")
public class MediaRequestEntity extends CreatedModifiedBaseEntity {

    public static final String ID_COLUMN_NAME = "mer_id";
    public static final String HEARING_ID_COLUMN_NAME = "hea_id";
    public static final String CURRENT_OWNER_COLUMN_NAME = "current_owner";
    public static final String REQUESTOR_COLUMN_NAME = "requestor";
    public static final String REQUEST_STATUS_COLUMN_NAME = "request_status";
    public static final String REQUEST_TYPE_COLUMN_NAME = "request_type";
    public static final String REQ_PROC_ATTEMPTS_COLUMN_NAME = "req_proc_attempts";
    public static final String START_TIME_COLUMN_NAME = "start_ts";
    public static final String END_TIME_COLUMN_NAME = "end_ts";
    public static final String CREATED_TS_COLUMN_NAME = "created_ts";
    public static final String CREATED_BY_COLUMN_NAME = "created_by";
    public static final String LAST_MODIFIED_TS_COLUMN_NAME = "last_modified_ts";
    public static final String LAST_MODIFIED_BY_COLUMN_NAME = "last_modified_by";
    public static final String TABLE_NAME = "media_request";

    @Id
    @Column(name = ID_COLUMN_NAME)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_request_gen")
    @SequenceGenerator(name = "media_request_gen", sequenceName = "mer_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    @Audited
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = HEARING_ID_COLUMN_NAME, nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private HearingEntity hearing;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = CURRENT_OWNER_COLUMN_NAME, nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private UserAccountEntity currentOwner;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = REQUESTOR_COLUMN_NAME, nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private UserAccountEntity requestor;

    @Column(name = REQUEST_STATUS_COLUMN_NAME, nullable = false)
    @Enumerated(EnumType.STRING)
    private MediaRequestStatus status;

    @Column(name = REQUEST_TYPE_COLUMN_NAME, nullable = false)
    @Enumerated(EnumType.STRING)
    @Audited
    private AudioRequestType requestType;

    @Column(name = REQ_PROC_ATTEMPTS_COLUMN_NAME)
    private Integer attempts;

    @Audited
    @Column(name = START_TIME_COLUMN_NAME, nullable = false)
    private OffsetDateTime startTime;

    @Audited
    @Column(name = END_TIME_COLUMN_NAME, nullable = false)
    private OffsetDateTime endTime;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = TransformedMediaEntity_.MEDIA_REQUEST)
    private List<TransformedMediaEntity> transformedMediaEntities = new ArrayList<>();
}