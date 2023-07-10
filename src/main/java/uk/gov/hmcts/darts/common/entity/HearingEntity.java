package uk.gov.hmcts.darts.common.entity;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "hearing")
@Getter
@Setter
public class HearingEntity {

    @Id
    @Column(name = "hea_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hea_gen")
    @SequenceGenerator(name = "hea_gen", sequenceName = "hea_seq", allocationSize = 1)
    private Integer id;

    @JoinColumn(name = "ctr_id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private CourtroomEntity courtroom;

    @Type(ListArrayType.class)
    @Column(name = "judge_list")
    private List<String> judges;

    @Column(name = "hearing_date")
    private LocalDate hearingDate;

    @Column(name = "scheduled_start_time")
    private LocalTime scheduledStartTime;

    @Column(name = "hearing_is_actual")
    private Boolean hearingIsActual;

    @Column(name = "judge_hearing_date")
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

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "cas_id")
    private CaseEntity courtCase;

    public boolean isFor(OffsetDateTime dateTime) {
        return hearingDate.equals(dateTime.toLocalDate());
    }
}
