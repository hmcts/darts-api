package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import uk.gov.hmcts.darts.util.DataUtil;

import java.time.OffsetDateTime;

@Entity
@Table(name = "arm_automated_task")
@Getter
@Setter
@EqualsAndHashCode
@Audited
@AuditTable("arm_automated_task_aud")
public class ArmAutomatedTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "aat_seq")
    @SequenceGenerator(name = "aat_seq", sequenceName = "aat_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    @Column(name = "aat_id", nullable = false)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aut_id", nullable = false)
    private AutomatedTaskEntity automatedTask;

    @Column(name = "rpo_csv_start_hour")
    private Integer rpoCsvStartHour;

    @Column(name = "rpo_csv_end_hour")
    private Integer rpoCsvEndHour;

    @Column(name = "arm_replay_start_ts")
    private OffsetDateTime armReplayStartTs;

    @Column(name = "arm_replay_end_ts")
    private OffsetDateTime armReplayEndTs;

    @Column(name = "arm_attribute_type")
    private String armAttributeType;

}
