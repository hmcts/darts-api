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
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "moj_hearing")
@Data
public class Hearing {

    @Id
    @Column(name = "moj_hea_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_hea_gen")
    @SequenceGenerator(name = "moj_hea_gen", sequenceName = "moj_hea_seq", allocationSize = 1)
    private Integer id;

    @JoinColumn(name = "moj_ctr_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Courtroom courtroom;

    @Type(ListArrayType.class)
    @Column(name = "c_judges")
    private List<String> judge;

    @Column(name = "c_hearing_date")
    private LocalDate hearingDate;

    @Column(name = "c_scheduled_start_time")
    private LocalTime scheduledStartTime;

    @Column(name = "hearing_is_actual")
    private Boolean hearingIsActual;

    @Column(name = "c_judge_hearing_date")
    private String judgeHearingDate;

    @ManyToMany
    @JoinTable(name = "moj_hearing_media_ae",
        joinColumns = {@JoinColumn(name = "moj_hea_id")},
        inverseJoinColumns = {@JoinColumn(name = "moj_med_id")})
    private List<Media> theMedias;

    @ManyToMany
    @JoinTable(name = "moj_hearing_event_ae",
        joinColumns = {@JoinColumn(name = "moj_hea_id")},
        inverseJoinColumns = {@JoinColumn(name = "moj_eve_id")})
    private List<Event> theEvents;

    @ManyToMany(mappedBy = "hearings")
    @Cascade({ CascadeType.SAVE_UPDATE, CascadeType.MERGE, CascadeType.PERSIST})
    private Set<Case> cases;
}
