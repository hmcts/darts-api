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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hearing")
@Getter
@Setter
public class HearingEntity extends CreatedModifiedBaseEntity {

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

    @Column(name = "judge_hearing_date")
    private String judgeHearingDate;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "hearing_judge_ae",
        joinColumns = {@JoinColumn(name = HEA_ID)},
        inverseJoinColumns = {@JoinColumn(name = "jud_id")})
    private List<JudgeEntity> judges = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "hearing_media_ae",
        joinColumns = {@JoinColumn(name = HEA_ID)},
        inverseJoinColumns = {@JoinColumn(name = "med_id")})
    private List<MediaEntity> mediaList = new ArrayList<>();

    @OneToMany(orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = TranscriptionEntity_.HEARING)
    private List<TranscriptionEntity> transcriptions = new ArrayList<>();

    @OneToMany(mappedBy = MediaRequestEntity_.HEARING)
    private List<MediaRequestEntity> mediaRequests = new ArrayList<>();

    @Transient
    private boolean isNew; //helper flag to indicate that the entity was just created, and so to notify DAR PC

    //TODO look to remove this
    @Deprecated()
    @ManyToMany
    @JoinTable(name = "hearing_event_ae",
        joinColumns = {@JoinColumn(name = HEA_ID)},
        inverseJoinColumns = {@JoinColumn(name = "eve_id")})
    private List<EventEntity> eventList = new ArrayList<>();

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    public void addMedia(MediaEntity mediaEntity) {
        mediaList.add(mediaEntity);
    }

    public void addJudge(JudgeEntity judgeEntity) {
        if (judgeEntity == null) {
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
            addJudge(judge);
        }
    }

    public List<String> getJudgesStringList() {
        return CollectionUtils.emptyIfNull(judges).stream().map(JudgeEntity::getName).toList();
    }
}
