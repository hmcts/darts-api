package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.SQLRestriction;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.task.runner.CanReturnExternalObjectDirectoryEntities;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;
import uk.gov.hmcts.darts.task.runner.HasRetention;
import uk.gov.hmcts.darts.task.runner.SoftDelete;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Entity
@Table(name = "media")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@SQLRestriction("is_deleted = false")
public class MediaEntity extends CreatedModifiedBaseEntity
    implements ConfidenceAware, SoftDelete, HasIntegerId, HasRetention, CanReturnExternalObjectDirectoryEntities {
    public static final Character MEDIA_TYPE_DEFAULT = 'A';

    @Id
    @Column(name = "med_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "med_gen")
    @SequenceGenerator(name = "med_gen", sequenceName = "med_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ctr_id", foreignKey = @ForeignKey(name = "media_courtroom_fk"), nullable = false)
    private CourtroomEntity courtroom;

    @Column(name = "media_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "channel", nullable = false)
    private Integer channel;

    @Column(name = "total_channels", nullable = false)
    private Integer totalChannels;

    @Column(name = "start_ts", nullable = false)
    private OffsetDateTime start;

    @Column(name = "end_ts", nullable = false)
    private OffsetDateTime end;

    @OneToMany(mappedBy = MediaLinkedCaseEntity_.MEDIA)
    private List<MediaLinkedCaseEntity> mediaLinkedCaseList = new ArrayList<>();

    @Column(name = "version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "media_file", nullable = false)
    private String mediaFile;

    @Column(name = "media_format", nullable = false)
    private String mediaFormat;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "media_type", nullable = false)
    private Character mediaType;

    @Column(name = "content_object_id", length = 16)
    private String contentObjectId;

    @Column(name = "clip_id")
    private String clipId;

    @Column(name = "chronicle_id")
    private String chronicleId;

    @Column(name = "antecedent_id")
    private String antecedentId;

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "is_current")
    private Boolean isCurrent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private UserAccountEntity deletedBy;

    @Column(name = "deleted_ts")
    private OffsetDateTime deletedTimestamp;

    @Column(name = "media_status")//leaving nullable for now
    private String mediaStatus;

    @ManyToMany(mappedBy = HearingEntity_.MEDIA_LIST)
    private List<HearingEntity> hearingList = new ArrayList<>();

    @Column(name = "retain_until_ts")
    private OffsetDateTime retainUntilTs;

    @OneToMany(mappedBy = ObjectAdminActionEntity_.MEDIA,
        fetch = FetchType.LAZY)
    private List<ObjectAdminActionEntity> objectAdminActions = new ArrayList<>();

    @Column(name = "ret_conf_score")
    private RetentionConfidenceScoreEnum retConfScore;

    @Column(name = "ret_conf_reason")
    private String retConfReason;

    @OneToMany(mappedBy = ExternalObjectDirectoryEntity_.MEDIA)
    private List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = new ArrayList<>();

    @Column(name = "subcontent_object_id", length = 16)
    private String subcontentObjectId;

    @Column(name = "subcontent_position")
    private Integer subcontentPosition;

    @Column(name = "data_ticket")
    private Integer dataTicket;

    @Column(name = "storage_id", length = 16)
    private String storageId;

    public List<CourtCaseEntity> associatedCourtCases() {
        var cases = hearingList.stream().map(HearingEntity::getCourtCase);
        return io.vavr.collection.List.ofAll(cases).distinctBy(CourtCaseEntity::getId).toJavaList();
    }

    public void removeHearing(HearingEntity hearing) {
        hearing.getMediaList().remove(this);
        getHearingList().remove(this);
    }

    @Override
    public void setDeletedTs(OffsetDateTime deletedTs) {
        setDeletedTimestamp(deletedTs);
    }

    @Override
    public OffsetDateTime getDeletedTs() {
        return getDeletedTimestamp();
    }

    public Optional<ObjectAdminActionEntity> getObjectAdminAction() {
        if (objectAdminActions.size() > 1) {
            log.warn("Media id {} has more than one admin action, yet the application logic expects Media->ObjectAdminAction is 1:1", id);
        }
        return objectAdminActions.stream().findFirst();
    }

    public void setObjectAdminAction(ObjectAdminActionEntity adminAction) {
        objectAdminActions.clear();
        objectAdminActions.add(adminAction);
    }

}