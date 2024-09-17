package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.task.runner.CanAnonymized;

import java.util.UUID;

@Entity
@Table(name = ProsecutorEntity.TABLE_NAME)
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ProsecutorEntity extends CreatedModifiedBaseEntity implements CanAnonymized {

    public static final String TABLE_NAME = "prosecutor";
    public static final String ID = "prn_id";
    public static final String CAS_ID = "cas_id";
    public static final String PROSECUTOR_NAME = "prosecutor_name";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prn_gen")
    @SequenceGenerator(name = "prn_gen", sequenceName = "prn_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = CAS_ID)
    private CourtCaseEntity courtCase;

    @Column(name = PROSECUTOR_NAME)
    private String name;

    @Override
    public void anonymize(UserAccountEntity userAccount) {
        this.setName(UUID.randomUUID().toString());
        this.setLastModifiedBy(userAccount);
    }
}
