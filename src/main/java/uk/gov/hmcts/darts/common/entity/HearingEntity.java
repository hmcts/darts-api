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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

@Entity
@Table(name = "hearing")
@Getter
@Setter
public class HearingEntity {

    public static final String HEA_ID = "hea_id";
    @Id
    @Column(name = HEA_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hea_gen")
    @SequenceGenerator(name = "hea_gen", sequenceName = "hea_seq", allocationSize = 1)
    private Integer id;

    @JoinColumn(name = "ctr_id")
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private CourtroomEntity courtroom;

    @Column(name = "hearing_date")
    private LocalDate hearingDate;

    @Column(name = "scheduled_start_time")
    private LocalTime scheduledStartTime;

    @Column(name = "hearing_is_actual")
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

    public boolean isFor(OffsetDateTime dateTime) {
        return hearingDate.equals(dateTime.toLocalDate());
    }

    public void addMedia(MediaEntity mediaEntity) {
        if (isNull(mediaList)) {
            mediaList = new ArrayList<>();
        }
        mediaList.add(mediaEntity);
    }

    public void addJudge(JudgeEntity judgeEntity) {
        courtCase.addJudge(judgeEntity);
        if (!judges.contains(judgeEntity)) {
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
