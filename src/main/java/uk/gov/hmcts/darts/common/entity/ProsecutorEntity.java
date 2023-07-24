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

@Entity
@Table(name = ProsecutorEntity.TABLE_NAME)
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ProsecutorEntity {

    public static final String TABLE_NAME = "prosecutors";
    public static final String ID = "pro_id";
    public static final String CAS_ID = "cas_id";
    public static final String PROSECUTOR_NAME = "prosecutor_name";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pro_gen")
    @SequenceGenerator(name = "pro_gen", sequenceName = "pro_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = CAS_ID)
    private CourtCaseEntity courtCase;

    @Column(name = PROSECUTOR_NAME)
    private String name;

}
