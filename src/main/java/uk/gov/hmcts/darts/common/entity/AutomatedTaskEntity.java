package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import uk.gov.hmcts.darts.common.entity.base.MandatoryCreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.util.DataUtil;

@Entity
@Table(name = AutomatedTaskEntity.TABLE_NAME)
@Getter
@Setter
@Audited
@AuditTable("automated_task_aud")
public class AutomatedTaskEntity extends MandatoryCreatedModifiedBaseEntity {

    public static final String TABLE_NAME = "automated_task";
    public static final String AUTOMATED_TASK_ID = "aut_id";
    public static final String TASK_NAME = "task_name";
    public static final String TASK_DESCRIPTION = "task_description";
    public static final String CRON_EXPRESSION = "cron_expression";
    public static final String CRON_EDITABLE = "cron_editable";
    private static final String TASK_GEN = "task_gen";
    private static final String AUTOMATED_TASK_SEQ = "aut_seq";
    private static final String TASK_ENABLED = "task_enabled";
    private static final String BATCH_SIZE = "batch_size";

    @Id
    @Column(name = AUTOMATED_TASK_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = TASK_GEN)
    @SequenceGenerator(name = TASK_GEN, sequenceName = AUTOMATED_TASK_SEQ, allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = TASK_NAME)
    private String taskName;

    @Column(name = TASK_DESCRIPTION)
    private String taskDescription;

    @Column(name = CRON_EXPRESSION)
    private String cronExpression;

    @Column(name = CRON_EDITABLE)
    private Boolean cronEditable;

    @Column(name = TASK_ENABLED)
    private Boolean taskEnabled;

    @Column(name = BATCH_SIZE)
    private Integer batchSize;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "automatedTask")
    @NotAudited
    private ArmAutomatedTaskEntity armAutomatedTaskEntity;
}