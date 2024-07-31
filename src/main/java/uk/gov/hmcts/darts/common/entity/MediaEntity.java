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
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "media")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class MediaEntity extends CreatedModifiedBaseEntity {
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
    private List<ObjectAdminActionEntity> adminActionReasons = new ArrayList<>();

    @Column(name = "ret_conf_score")
    private Integer retConfScore;

    @Column(name = "ret_conf_reason")
    private String retConfReason;

    public List<CourtCaseEntity> associatedCourtCases() {
        var cases = hearingList.stream().map(HearingEntity::getCourtCase);
        return io.vavr.collection.List.ofAll(cases).distinctBy(CourtCaseEntity::getId).toJavaList();
    }

    public void removeHearing(HearingEntity hearing) {
        hearing.getMediaList().remove(this);
    }
}