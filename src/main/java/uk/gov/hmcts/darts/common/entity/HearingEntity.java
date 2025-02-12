package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.annotations.SortNatural;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity_;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;
import uk.gov.hmcts.darts.util.DataUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

@Entity
@Table(name = "hearing")
@Getter
@Setter
@Slf4j
public class HearingEntity extends CreatedModifiedBaseEntity
    implements HasIntegerId, Comparable<HearingEntity> {

    public static final String HEA_ID = "hea_id";
    @Id
    @Column(name = HEA_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hea_gen")
    @SequenceGenerator(name = "hea_gen", sequenceName = "hea_seq", allocationSize = 1)
    private Integer id;

    @JoinColumn(name = "ctr_id")
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private CourtroomEntity courtroom;

    @Column(name = "hearing_date", nullable = false)
    private LocalDate hearingDate;

    @Column(name = "scheduled_start_time")
    private LocalTime scheduledStartTime;

    @Column(name = "hearing_is_actual", nullable = false)
    private Boolean hearingIsActual;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "hearing_judge_ae",
        joinColumns = {@JoinColumn(name = HEA_ID)},
        inverseJoinColumns = {@JoinColumn(name = "jud_id")})
    private List<JudgeEntity> judges = new ArrayList<>();

    @ManyToMany(cascade = CascadeType.REMOVE)
    @JoinTable(name = "hearing_media_ae",
        joinColumns = {@JoinColumn(name = HEA_ID)},
        inverseJoinColumns = {@JoinColumn(name = "med_id")})
    @SortNatural
    private SortedSet<MediaEntity> medias = new TreeSet<>();

    @ManyToMany(fetch = FetchType.EAGER, mappedBy = TranscriptionEntity_.HEARINGS)
    private List<TranscriptionEntity> transcriptions = new ArrayList<>();

    @OneToMany(mappedBy = MediaRequestEntity_.HEARING)
    private List<MediaRequestEntity> mediaRequests = new ArrayList<>();

    @Transient
    private boolean isNew; //helper flag to indicate that the entity was just created, and so to notify DAR PC

    @ManyToMany
    @JoinTable(name = "hearing_event_ae",
        joinColumns = {@JoinColumn(name = HEA_ID)},
        inverseJoinColumns = {@JoinColumn(name = "eve_id")})
    private List<EventEntity> eventList = new ArrayList<>();

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    @ManyToMany
    @JoinTable(name = "hearing_annotation_ae",
        joinColumns = {@JoinColumn(name = HEA_ID)},
        inverseJoinColumns = {@JoinColumn(name = "ann_id")})
    private List<AnnotationEntity> annotations = new ArrayList<>();

    /**
     * Adds a media to the hearing.
     *
     * @param mediaEntity the media to add
     * @return true if the media was added, false if it was already present
     */
    public boolean addMedia(MediaEntity mediaEntity) {
        if (containsMedia(mediaEntity)) {
            log.info("Media {} already exists in hearing {}", mediaEntity.getId(), id);
            return false;
        }
        log.info("Added media {} to hearing {}", mediaEntity.getId(), id);
        medias.add(mediaEntity);
        return true;
    }

    /**
     * Returns a unmodifiable list of medias associated with this hearing.
     * @deprecated use {@link #getMedias()} instead
     */
    @Deprecated(since = "2025-02-11")
    public List<MediaEntity> getMediaList() {
        List<MediaEntity> mediaEntities = new ArrayList<>();
        if (medias != null) {
            mediaEntities.addAll(medias);
        }
        return Collections.unmodifiableList(mediaEntities);
    }

    public void addJudge(JudgeEntity judgeEntity, boolean isFromDailyList) {
        if (judgeEntity == null || (!Boolean.TRUE.equals(hearingIsActual) && !isFromDailyList)) {
            return;
        }
        courtCase.addJudge(judgeEntity);
        if (!judges.contains(judgeEntity)
            && judges.stream().noneMatch(judge -> judge.getName().equalsIgnoreCase(judgeEntity.getName()))) {
            judges.add(judgeEntity);
        }
    }

    public void addJudges(List<JudgeEntity> judges) {
        for (JudgeEntity judge : judges) {
            addJudge(judge, false);
        }
    }

    public List<String> getJudgesStringList() {
        return CollectionUtils.emptyIfNull(judges).stream().map(JudgeEntity::getName).toList();
    }

    public void addAnnotation(AnnotationEntity annotationEntity) {
        annotations.add(annotationEntity);
    }

    public boolean containsMedia(MediaEntity mediaEntity) {
        return mediaEntity.getId() != null && medias.stream().anyMatch(media -> mediaEntity.getId().equals(media.getId()));
    }

    @Override
    public int compareTo(HearingEntity o) {
        return DataUtil.compareInteger(this.id, o.id);
    }

    public void removeMedia(MediaEntity mediaEntity) {
        medias.remove(mediaEntity);
    }
}
