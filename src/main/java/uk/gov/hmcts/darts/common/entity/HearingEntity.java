package uk.gov.hmcts.darts.common.entity;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
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
import lombok.Data;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "hearing")
@Data
public class HearingEntity {

    @Id
    @Column(name = "hea_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hea_gen")
    @SequenceGenerator(name = "hea_gen", sequenceName = "hea_seq", allocationSize = 1)
    private Integer id;

    @JoinColumn(name = "ctr_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private CourtroomEntity courtroom;

    @Type(ListArrayType.class)
    @Column(name = "c_judges")
    private List<String> judges;

    @Column(name = "c_hearing_date")
    private LocalDate hearingDate;

    @Column(name = "c_scheduled_start_time")
    private LocalTime scheduledStartTime;

    @Column(name = "hearing_is_actual")
    private Boolean hearingIsActual;

    @Column(name = "c_judge_hearing_date")
    private String judgeHearingDate;

    @ManyToMany
    @JoinTable(name = "hearing_media_ae",
        joinColumns = {@JoinColumn(name = "hea_id")},
        inverseJoinColumns = {@JoinColumn(name = "med_id")})
    private List<MediaEntity> mediaList;

    @ManyToMany
    @JoinTable(name = "hearing_event_ae",
        joinColumns = {@JoinColumn(name = "hea_id")},
        inverseJoinColumns = {@JoinColumn(name = "eve_id")})
    private List<EventEntity> eventList;

    @ManyToOne()
    @JoinColumn(name = "cas_id")
    private CaseEntity courtCase;
}
