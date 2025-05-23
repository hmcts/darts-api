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
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity_;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "hearing")
@Getter
@Setter
public class HearingEntity extends CreatedModifiedBaseEntity
    implements HasIntegerId {

    public static final String HEA_ID = "hea_id";
    @Id
    @Column(name = HEA_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hea_gen")
    @SequenceGenerator(name = "hea_gen", sequenceName = "hea_seq", allocationSize = 1)
    private Integer id;

    @JoinColumn(name = "ctr_id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private CourtroomEntity courtroom;

    @Column(name = "hearing_date", nullable = false)
    private LocalDate hearingDate;

    @Column(name = "scheduled_start_time")
    private LocalTime scheduledStartTime;

    @Column(name = "hearing_is_actual", nullable = false)
    private Boolean hearingIsActual;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "hearing_judge_ae",
        joinColumns = {@JoinColumn(name = HEA_ID)},
        inverseJoinColumns = {@JoinColumn(name = "jud_id")})
    private Set<JudgeEntity> judges = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "hearing_media_ae",
        joinColumns = {@JoinColumn(name = HEA_ID)},
        inverseJoinColumns = {@JoinColumn(name = "med_id")})
    private Set<MediaEntity> medias = new HashSet<>();

    @ManyToMany(mappedBy = TranscriptionEntity_.HEARINGS)
    private Set<TranscriptionEntity> transcriptions = new HashSet<>();

    @OneToMany(mappedBy = MediaRequestEntity_.HEARING)
    private Set<MediaRequestEntity> mediaRequests = new HashSet<>();

    @Transient
    private boolean isNew; //helper flag to indicate that the entity was just created, and so to notify DAR PC

    @ManyToMany
    @JoinTable(name = "hearing_event_ae",
        joinColumns = {@JoinColumn(name = HEA_ID)},
        inverseJoinColumns = {@JoinColumn(name = "eve_id")})
    private Set<EventEntity> events = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    @ManyToMany
    @JoinTable(name = "hearing_annotation_ae",
        joinColumns = {@JoinColumn(name = HEA_ID)},
        inverseJoinColumns = {@JoinColumn(name = "ann_id")})
    private Set<AnnotationEntity> annotations = new HashSet<>();

    public void addMedia(MediaEntity mediaEntity) {
        if (!containsMedia(mediaEntity)) {
            medias.add(mediaEntity);
        }
    }

    public void addEvent(EventEntity eventEntity) {
        if (!containsEvent(eventEntity)) {
            events.add(eventEntity);
        }
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

    public void addJudges(Collection<JudgeEntity> judges) {
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

    public boolean containsEvent(EventEntity eventEntity) {
        return eventEntity.getId() != null && events.stream().anyMatch(event -> eventEntity.getId().equals(event.getId()));

    }
}
