package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "moj_courthouse")
@Data
public class Courthouse {

    @Id
    @Column(name = "moj_crt_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code")
    private short code;

    @Column(name = "courthouse_name")
    private String courthouseName;

    @CreationTimestamp
    @Column(name = "created_date_time")
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_modified_date_time")
    private Timestamp lastModifiedDateTime;
}
