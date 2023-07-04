package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "courthouse")
@Data
public class CourthouseEntity {

    @Id
    @Column(name = "cth_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cth_gen")
    @SequenceGenerator(name = "cth_gen", sequenceName = "cth_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "courthouse_code", unique = true)
    private Integer code;

    @Column(name = "courthouse_name", unique = true)
    private String courthouseName;

    @CreationTimestamp
    @Column(name = "created_ts")
    private OffsetDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_modified_ts")
    private OffsetDateTime lastModifiedDateTime;

}
